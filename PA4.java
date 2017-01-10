//****************************************************************************
//       Main Program for CS480 PA4
//****************************************************************************
// Description: 
//   
//   This is a template program for the sketching tool.  
//
//     LEFTMOUSE: draw line segments 
//     RIGHTMOUSE: draw triangles 
//
//     The following keys control the program:
//
//      Q,q: quit 
//      C,c: clear polygon (set vertex count=0)
//		R,r: randomly change the color
//		S,s: toggle the smooth shading
//		T,t: show testing examples (toggles between smooth shading and flat shading test cases)
//		>:	 increase the step number for examples
//		<:   decrease the step number for examples
//     +,-:  increase or decrease spectral exponent
//      H: increase intensity of ambient
//      h: decrease intensity of ambient
//      J: increase intensity of specular
//      j: decrease intensity of specular
//      K: increase intensity of diffusion
//      k: decrease intensity of diffusion
//
//****************************************************************************
// History :
//   Aug 2004 Created by Jianming Zhang based on the C
//   code by Stan Sclaroff
//   Nov 2014 modified to include test cases for shading example for PA4
//   Dec 2016 Edited by Ryan Walsh for PA4
//


import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.*; 
import java.awt.image.*;
//import java.io.File;
//import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

//import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;//for new version of gl
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import com.jogamp.opengl.util.FPSAnimator;//for new version of gl


public class PA4 extends JFrame
	implements GLEventListener, KeyListener, MouseListener, MouseMotionListener
{
	
	private static final long serialVersionUID = 1L;
	private final int DEFAULT_WINDOW_WIDTH=512;
	private final int DEFAULT_WINDOW_HEIGHT=512;
	private final float DEFAULT_LINE_WIDTH=1.0f;

	private GLCapabilities capabilities;
	private GLCanvas canvas;
	private FPSAnimator animator;

	final private int numTestCase;
	private int testCase;
	private BufferedImage buff;
	@SuppressWarnings("unused")
	private ColorType color;
	private Random rng;
	
	 // specular exponent for materials
	private int ns=5; 
	
	private ArrayList<Point2D> lineSegs;
	private ArrayList<Point2D> triangles;
	private boolean doSmoothShading;
	private int Nsteps;

	/** The quaternion which controls the rotation of the world. */
	private Quaternion viewing_quaternion = new Quaternion();
	private Vector3D viewing_center = new Vector3D((float)(DEFAULT_WINDOW_WIDTH/2),(float)(DEFAULT_WINDOW_HEIGHT/2),(float)0.0);
	/** The last x and y coordinates of the mouse press. */
	private int last_x = 0, last_y = 0;
	/** Whether the world is being rotated. */
	private boolean rotate_world = false;

	private boolean[] matTerms;
	private List<Light> lights;
	private boolean lightMade;
	private boolean lPressed;

	private float factorA = 1;
	private float factorS = 1;
	private float factorD = 1;

	private float[][] depthBuffer;
	
	public PA4()
	{
		capabilities = new GLCapabilities(null);
		capabilities.setDoubleBuffered(true);  // Enable Double buffering

		canvas  = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
		canvas.setAutoSwapBufferMode(true); // true by default. Just to be explicit
		canvas.setFocusable(true);
		getContentPane().add(canvas);

		animator = new FPSAnimator(canvas, 60); // drive the display loop @ 60 FPS

		numTestCase = 3;
		testCase = 0;
		Nsteps = 12;

		setTitle("CS480/680 PA4");
		setSize( DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setResizable(false);
		
		rng = new Random();
		color = new ColorType(1.0f,0.0f,0.0f);
		lineSegs = new ArrayList<Point2D>();
		triangles = new ArrayList<Point2D>();
		doSmoothShading = true;

		matTerms = new boolean[3];
	}

	public void run()
	{
		animator.start();
	}

	public static void main( String[] args )
	{
		PA4 P = new PA4();
		P.run();
	}

	//*********************************************** 
	//  GLEventListener Interfaces
	//*********************************************** 
	public void init( GLAutoDrawable drawable) 
	{
		lightMade = false;
		lPressed = false;

		GL gl = drawable.getGL();
		gl.glClearColor( 0.0f, 0.0f, 0.0f, 0.0f);
		gl.glLineWidth( DEFAULT_LINE_WIDTH );
		Dimension sz = this.getContentPane().getSize();
		buff = new BufferedImage(sz.width,sz.height,BufferedImage.TYPE_3BYTE_BGR);
		clearPixelBuffer();

		Arrays.fill(matTerms, true);
	}

	// Redisplaying graphics
	public void display(GLAutoDrawable drawable)
	{
		setDepthBuffer();

		GL2 gl = drawable.getGL().getGL2();
		WritableRaster wr = buff.getRaster();
		DataBufferByte dbb = (DataBufferByte) wr.getDataBuffer();
		byte[] data = dbb.getData();

		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
		gl.glDrawPixels (buff.getWidth(), buff.getHeight(),
				GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE,
				ByteBuffer.wrap(data));
		drawTestCase();
	}

	public void setDepthBuffer()
	{
		depthBuffer = new float[DEFAULT_WINDOW_WIDTH][DEFAULT_WINDOW_HEIGHT];

		for(int i = 0; i < depthBuffer.length; i++)
		{
			for(int j = 0; j < depthBuffer[0].length; j++)
			{
				depthBuffer[i][j] = -99999;
			}
		}
	}

	// Window size change
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		// deliberately left blank
	}
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
		  boolean deviceChanged)
	{
		// deliberately left blank
	}
	
	void clearPixelBuffer()
	{
		setDepthBuffer();

		lineSegs.clear();
		triangles.clear();
		Graphics2D g = buff.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, buff.getWidth(), buff.getHeight());
		g.dispose();

	}
	
	// drawTest
	void drawTestCase()
	{  
		/* clear the window and vertex state */
		clearPixelBuffer();
	  
		//System.out.printf("Test case = %d\n",testCase);

		switch (testCase){
		case 0:
			scene1();
			break;
		case 1:
			scene2();
			break;
		case 2:
			scene3();
			break;
		}	
	}


	//*********************************************** 
	//          KeyListener Interfaces
	//*********************************************** 
	public void keyTyped(KeyEvent key)
	{
	//      Q,q: quit 
	//      C,c: clear polygon (set vertex count=0)
	//		R,r: randomly change the color
	//		S,s: toggle the smooth shading
	//		T,t: show testing examples (toggles between smooth shading and flat shading test cases)
	//		>:	 increase the step number for examples
	//		<:   decrease the step number for examples
	//     +,-:  increase or decrease spectral exponent
	//      H: increase intensity of ambient
	//      h: decrease intensity of ambient
	//      J: increase intensity of specular
	//      j: decrease intensity of specular
	//      K: increase intensity of diffusion
	//      k: decrease intensity of diffusion

		switch ( key.getKeyChar() ) 
		{
		case 'L':
			lPressed = !lPressed;
			break;
		case '1':
			if(lPressed) lights.get(0).changeState();
			break;
		case '2':
			if(lPressed) lights.get(1).changeState();
			break;
		case '3':
			if(lPressed) lights.get(2).changeState();
			break;
		case 'Q' :
		case 'q' : 
			new Thread()
			{
				public void run() { animator.stop(); }
			}.start();
			System.exit(0);
			break;
		case 'R' :
		case 'r' :
			color = new ColorType(rng.nextFloat(),rng.nextFloat(),
					rng.nextFloat());
			break;
		case 'C' :
		case 'c' :
			clearPixelBuffer();
			break;
		case 'S' :
		case 's' :
			matTerms[0] = !matTerms[0];
			break;
		case 'D' :
		case 'd' :
			matTerms[1] = !matTerms[1];
			break;
		case 'A' :
		case 'a' :
			matTerms[2] = !matTerms[2];
			break;
		case 'T' :
		case 't' : 
			testCase = (testCase+1)%numTestCase;
			lightMade = false;
			drawTestCase();
			break; 
		case '<':  
			Nsteps = Nsteps < 4 ? Nsteps: Nsteps / 2;
			System.out.printf( "Nsteps = %d \n", Nsteps);
			drawTestCase();
			break;
		case '>':
			Nsteps = Nsteps > 190 ? Nsteps: Nsteps * 2;
			System.out.printf( "Nsteps = %d \n", Nsteps);
			drawTestCase();
			break;
		case '+':
			ns++;
			drawTestCase();
			break;
		case '-':
			if(ns>0)
				ns--;
			drawTestCase();
			break;
		case 'H':
			factorA +=1;
			drawTestCase();
			break;
		case 'h':
			if(factorA > 0) factorA -=1;
			drawTestCase();
			break;
		case 'J':
			factorS +=1;
			drawTestCase();
			break;
		case 'j':
			if(factorS > 0) factorS -=1;
			drawTestCase();
			break;
		case 'K':
			factorD +=1;
			drawTestCase();
			break;
		case 'k':
			if(factorD > 0) factorD -=1;
			drawTestCase();
			break;
		case 'F':
		case 'f':
			doSmoothShading = false;;
			drawTestCase();
			break;
		case 'G':
		case 'g':
			doSmoothShading = true;
			drawTestCase();
			break;
		default :
			break;
		}
	}

	public void keyPressed(KeyEvent key)
	{
		switch (key.getKeyCode()) 
		{
		case KeyEvent.VK_ESCAPE:
			new Thread()
			{
				public void run()
				{
					animator.stop();
				}
			}.start();
			System.exit(0);
			break;
		  default:
			break;
		}
	}

	public void keyReleased(KeyEvent key)
	{
		// deliberately left blank
	}

	//************************************************** 
	// MouseListener and MouseMotionListener Interfaces
	//************************************************** 
	public void mouseClicked(MouseEvent mouse)
	{
		// deliberately left blank
	}
	  public void mousePressed(MouseEvent mouse)
	  {
		int button = mouse.getButton();
		if ( button == MouseEvent.BUTTON1 )
		{
		  last_x = mouse.getX();
		  last_y = mouse.getY();
		  rotate_world = true;
		}
	  }

	  public void mouseReleased(MouseEvent mouse)
	  {
		int button = mouse.getButton();
		if ( button == MouseEvent.BUTTON1 )
		{
		  rotate_world = false;
		}
	  }

	public void mouseMoved( MouseEvent mouse)
	{
		// Deliberately left blank
	}

	/**
	   * Updates the rotation quaternion as the mouse is dragged.
	   * 
	   * @param mouse
	   *          The mouse drag event object.
	   */
	  public void mouseDragged(final MouseEvent mouse) {
		if (this.rotate_world) {
		  // get the current position of the mouse
		  final int x = mouse.getX();
		  final int y = mouse.getY();

		  // get the change in position from the previous one
		  final int dx = x - this.last_x;
		  final int dy = y - this.last_y;

		  // create a unit vector in the direction of the vector (dy, dx, 0)
		  final float magnitude = (float)Math.sqrt(dx * dx + dy * dy);
		  if(magnitude > 0.0001)
		  {
			  // define axis perpendicular to (dx,-dy,0)
			  // use -y because origin is in upper lefthand corner of the window
			  final float[] axis = new float[] { -(float) (dy / magnitude),
					  (float) (dx / magnitude), 0 };

			  // calculate appropriate quaternion
			  final float viewing_delta = 3.1415927f / 180.0f;
			  final float s = (float) Math.sin(0.5f * viewing_delta);
			  final float c = (float) Math.cos(0.5f * viewing_delta);
			  final Quaternion Q = new Quaternion(c, s * axis[0], s * axis[1], s
					  * axis[2]);
			  this.viewing_quaternion = Q.multiply(this.viewing_quaternion);

			  // normalize to counteract acccumulating round-off error
			  this.viewing_quaternion.normalize();

			  // save x, y as last x, y
			  this.last_x = x;
			  this.last_y = y;
			  drawTestCase();
		  }
		}

	  }
	  
	public void mouseEntered( MouseEvent mouse)
	{
		// Deliberately left blank
	}

	public void mouseExited( MouseEvent mouse)
	{
		// Deliberately left blank
	} 


	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub
		
	}

	public ColorType applyAllLights(List<Light> lights, Material mat, Vector3D v, Vector3D n, Vector3D ps)
	{
		Light l;
		ColorType res = new ColorType();
		// temporarily holds RGB values
		ColorType temp = new ColorType();

		// loops through all lights in a given scene
		for(int i = 0; i < lights.size(); i++)
		{
			l = lights.get(i);

			if(l.isOn){
				temp = l.applyLight(mat, v, n, ps);

				// adds up all color for each channel
				res.r += temp.r;
				res.g += temp.g;
				res.b += temp.b;
			}
		}

		// clamp so that allowable maximum illumination level is not exceeded
		res.r = (float) Math.min(1.0, res.r);
		res.g = (float) Math.min(1.0, res.g);
		res.b = (float) Math.min(1.0, res.b);

		return res;

	}
	
	//************************************************** 
	// Test Cases
	// Nov 9, 2014 Stan Sclaroff -- removed line and triangle test cases
	//************************************************** 

	void scene1(){
		
		// all objects in scene
		float radius = (float)50.0;
		Sphere3D sphere = new Sphere3D(128.0f, 128.0f, 128.0f, 1.5f*radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D(256.0f, 384.0f, 128.0f, 0.8f*radius, 1.25f*radius, Nsteps, Nsteps);
		Ellipsoid ellip = new Ellipsoid(350f, 100f, 50f, 1.5f*radius, radius, radius, Nsteps, Nsteps);
		Cylinder cylinder = new Cylinder(350f, 50f, 200f, radius, radius, 1.5f*radius, Nsteps, Nsteps);
		Superellipsoid superBox = new Superellipsoid(300f, 200f, 0f, radius, radius, radius, Nsteps, Nsteps, .1);
		Superellipsoid superEllip = new Superellipsoid(300f, 350f, 0f, 1.5f*radius, radius, radius, Nsteps, Nsteps, .8);
	   
		// view vector is defined along z axis
		// this example assumes simple othorgraphic projection
		// view vector is used in 
		//   (a) calculating specular lighting contribution
		//   (b) backface culling / backface rejection
		Vector3D view_vector = new Vector3D((float)0.0,(float)0.0,(float)1.0);
	  
		// material properties for the sphere and torus
		// ambient, diffuse, and specular coefficients
		// specular exponent is a global variable
		ColorType torus_ka = new ColorType(0.3f*factorA,0.3f*factorA,0.3f*factorA);
		ColorType sphere_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType ellip_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType cyl_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType super_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);

		ColorType torus_kd = new ColorType(0.0f*factorD,0.5f*factorD,0.9f*factorD);
		ColorType sphere_kd = new ColorType(0.9f*factorD,0.3f*factorD,0.1f*factorD);
		ColorType ellip_kd = new ColorType(0.1f*factorD,0.9f*factorD,0.01f*factorD);
		ColorType cyl_kd = new ColorType(1.9f*factorD,1.0f*factorD,0.0f*factorD);
		ColorType super_kd = new ColorType(1.0f*factorD,1.0f*factorD,1.0f*factorD);

		ColorType torus_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType sphere_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType ellip_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType cyl_ks = new ColorType(0.0f*factorS,0.0f*factorS,0.0f*factorS);
		ColorType super_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);

		// material properties stored in array
		Material[] mats = {
			new Material(matTerms, sphere_ka, sphere_kd, sphere_ks, ns),
			new Material(matTerms, torus_ka, torus_kd, torus_ks, ns),
			new Material(matTerms, ellip_ka, ellip_kd, ellip_ks, ns),
			new Material(matTerms, cyl_ka, cyl_kd, cyl_ks, ns),
			new Material(matTerms, super_ka, super_kd, super_ks, ns),
			new Material(matTerms, super_ka, super_kd, super_ks, ns)};


		// if we have not already made the lights for this scene
		if(!lightMade) {

			lightMade = true;

			lights = new ArrayList<Light>();

			// define one infinite light source, with color = white
			ColorType light_color = new ColorType(1.0f,1.0f,1.0f);
			Vector3D light_direction = new Vector3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
			Light light = new Light(light_color,light_direction, false, true);

			lights.add(light);

			// define one ambient light source, with color = white
			light_color = new ColorType(1.0f,1.0f,1.0f);
			light_direction = new Vector3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
			Light ambient = new Light(light_color,light_direction, true, false);

			lights.add(ambient);

			// point light with color = red
			light_color = new ColorType(1.0f,0.0f,0.0f);
			light_direction = new Vector3D(0f, -1f, 1f);
			Vector3D light_position = new Vector3D(0f, 400f, 200f);
			Light spot = new Light(light_color,light_direction, light_position);

			lights.add(spot);

		}
		
		// normal to the plane of a triangle
		// to be used in backface culling / backface rejection
		Vector3D triangle_normal = new Vector3D();
		
		// a triangle mesh
		Mesh3D mesh;
			
		int i, j, n, m;
		
		// temporary variables for triangle 3D vertices and 3D normals
		Vector3D v0,v1, v2, n0, n1, n2;
		
		// projected triangle, with vertex colors
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};
		
		for(int k=0;k<6;++k) // loop through all objects in scene
		{
			if(k == 0)
			{
				mesh=sphere.mesh;
				n=sphere.get_n();
				m=sphere.get_m();
			}
			else if(k == 1)
			{
				mesh=torus.mesh;
				n=torus.get_n();
				m=torus.get_m();
			}
			else if(k == 2)
			{
				mesh = ellip.mesh;
				n = ellip.get_n();
				m = ellip.get_m();
			}
			else if(k == 3)
			{
				mesh = cylinder.mesh;
				n = cylinder.get_n();
				m = cylinder.get_m();
			}
			else if (k == 4)
			{
				mesh = superEllip.mesh;
				n = superEllip.get_n();
				m = superEllip.get_m();
			}
			else
			{
				mesh = superBox.mesh;
				n = superBox.get_n();
				m = superBox.get_m();
			}
			
			// rotate the surface's 3D mesh using quaternion
			mesh.rotateMesh(viewing_quaternion, viewing_center);
					
			// draw triangles for the current surface, using vertex colors
			// this works for Gouraud and flat shading only (not Phong)
			for(i=0; i < m-1; ++i)
			{
				for(j=0; j < n-1; ++j)
				{
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j+1];
					v2 = mesh.v[i+1][j+1];
					triangle_normal = computeTriangleNormal(v0,v1,v2);
					
					if(view_vector.dotProduct(triangle_normal) > 0.0)  // front-facing triangle?
					{	
						if(doSmoothShading)  
						{
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}
						else 
						{
							// flat shading: use the normal to the triangle itself
							n2 = n1 = n0 =  triangle_normal;
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}

						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[0].z = (int)v0.z;

						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[1].z = (int)v1.z;

						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						tri[2].z = (int)v2.z;
	
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],doSmoothShading,depthBuffer);      
					}
					
					v0 = mesh.v[i][j];
					v1 = mesh.v[i+1][j+1];
					v2 = mesh.v[i+1][j];
					triangle_normal = computeTriangleNormal(v0,v1,v2);
					
					if(view_vector.dotProduct(triangle_normal) > 0.0)  // front-facing triangle?
					{	
						if(doSmoothShading)
						{
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}
						else 
						{
							// flat shading: use the normal to the triangle itself
							n2 = n1 = n0 =  triangle_normal;
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}	
			
						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],doSmoothShading,depthBuffer);      
					}
				}	
			}
		}
	}

	void scene2(){
		
		// all objects in scene
		float radius = (float)50.0;
		Sphere3D sphere = new Sphere3D(50.0f, 100.0f, 250f, .5f*radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D(256.0f, 100.0f, 50f, 0.8f*radius, 2f*radius, Nsteps, Nsteps);
		Ellipsoid ellip = new Ellipsoid(350f, 380f, 130f, 1.5f*radius, radius, radius, Nsteps, Nsteps);
		Superellipsoid superEllip = new Superellipsoid(50f, 150f, 0f, radius, radius, radius, Nsteps, Nsteps, .8);
	   
		// view vector is defined along z axis
		// this example assumes simple othorgraphic projection
		// view vector is used in 
		//   (a) calculating specular lighting contribution
		//   (b) backface culling / backface rejection
		Vector3D view_vector = new Vector3D((float)0.0,(float)0.0,(float)1.0);
	  
		// material properties for the sphere and torus
		// ambient, diffuse, and specular coefficients
		// specular exponent is a global variable
		ColorType torus_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType sphere_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType ellip_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType super_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);

		ColorType torus_kd = new ColorType(0.0f*factorD,0.5f*factorD,0.9f*factorD);
		ColorType sphere_kd = new ColorType(0.9f*factorD,0.3f*factorD,0.1f*factorD);
		ColorType ellip_kd = new ColorType(0.1f*factorD,0.9f*factorD,0.01f*factorD);
		ColorType super_kd = new ColorType(1.0f*factorD,1.0f*factorD,1.0f*factorD);

		ColorType torus_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType sphere_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType ellip_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType super_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);

		// material properties stored in array
		Material[] mats = {
			new Material(matTerms, sphere_ka, sphere_kd, sphere_ks, ns),
			new Material(matTerms, torus_ka, torus_kd, torus_ks, ns),
			new Material(matTerms, ellip_ka, ellip_kd, ellip_ks, ns),
			new Material(matTerms, super_ka, super_kd, super_ks, ns)};


		// if we have not already made the lights for this scene
		if(!lightMade) {

			lightMade = true;

			lights = new ArrayList<Light>();

			// define one infinite light source, with color = white
			ColorType light_color = new ColorType(1.0f,1.0f,1.0f);
			Vector3D light_direction = new Vector3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
			Light light = new Light(light_color,light_direction, false, true);

			lights.add(light);

			// define one ambient light source, with color = white
			light_color = new ColorType(1.0f,1.0f,1.0f);
			light_direction = new Vector3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
			Light ambient = new Light(light_color,light_direction, true, false);

			lights.add(ambient);

			// point light with color = red
			light_color = new ColorType(0.0f,0.0f,1.0f);
			light_direction = new Vector3D(0f, -1f, 1f);
			Vector3D light_position = new Vector3D(400f, 200f, 0f);
			Light spot = new Light(light_color,light_direction, light_position);

			lights.add(spot);

		}
		
		// normal to the plane of a triangle
		// to be used in backface culling / backface rejection
		Vector3D triangle_normal = new Vector3D();
		
		// a triangle mesh
		Mesh3D mesh;
			
		int i, j, n, m;
		
		// temporary variables for triangle 3D vertices and 3D normals
		Vector3D v0,v1, v2, n0, n1, n2;
		
		// projected triangle, with vertex colors
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};
		
		for(int k=0;k<4;++k) // loop through all objects in scene
		{
			if(k == 0)
			{
				mesh=sphere.mesh;
				n=sphere.get_n();
				m=sphere.get_m();
			}
			else if(k == 1)
			{
				mesh=torus.mesh;
				n=torus.get_n();
				m=torus.get_m();
			}
			else if(k == 2)
			{
				mesh = ellip.mesh;
				n = ellip.get_n();
				m = ellip.get_m();
			}
			else
			{
				mesh = superEllip.mesh;
				n = superEllip.get_n();
				m = superEllip.get_m();
			}

			
			// rotate the surface's 3D mesh using quaternion
			mesh.rotateMesh(viewing_quaternion, viewing_center);
					
			// draw triangles for the current surface, using vertex colors
			// this works for Gouraud and flat shading only (not Phong)
			for(i=0; i < m-1; ++i)
			{
				for(j=0; j < n-1; ++j)
				{
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j+1];
					v2 = mesh.v[i+1][j+1];
					triangle_normal = computeTriangleNormal(v0,v1,v2);
					
					if(view_vector.dotProduct(triangle_normal) > 0.0)  // front-facing triangle?
					{	
						if(doSmoothShading)  
						{
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}
						else 
						{
							// flat shading: use the normal to the triangle itself
							n2 = n1 = n0 =  triangle_normal;
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}

						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[0].z = (int)v0.z;

						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[1].z = (int)v1.z;

						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						tri[2].z = (int)v2.z;
	
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],doSmoothShading,depthBuffer);      
					}
					
					v0 = mesh.v[i][j];
					v1 = mesh.v[i+1][j+1];
					v2 = mesh.v[i+1][j];
					triangle_normal = computeTriangleNormal(v0,v1,v2);
					
					if(view_vector.dotProduct(triangle_normal) > 0.0)  // front-facing triangle?
					{	
						if(doSmoothShading)
						{
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}
						else 
						{
							// flat shading: use the normal to the triangle itself
							n2 = n1 = n0 =  triangle_normal;
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}	
			
						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],doSmoothShading,depthBuffer);      
					}
				}	
			}
		}
	}

	void scene3(){
		
		// all objects in scene
		float radius = (float)50.0;
		Sphere3D sphere = new Sphere3D(50.0f, 100.0f, 250f, 2.0f*radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D(50.0f, 150.0f, 50f, .4f*radius, radius, Nsteps, Nsteps);
		Ellipsoid ellip = new Ellipsoid(350f, 130, 300, .75f*radius, .5f*radius, .5f*radius, Nsteps, Nsteps);
		Superellipsoid superEllip = new Superellipsoid(250, 75f, 0f, .75f*radius, .5f*radius, .5f*radius, Nsteps, Nsteps, .8);
	   
		// view vector is defined along z axis
		// this example assumes simple othorgraphic projection
		// view vector is used in 
		//   (a) calculating specular lighting contribution
		//   (b) backface culling / backface rejection
		Vector3D view_vector = new Vector3D((float)0.0,(float)0.0,(float)1.0);
	  
		// material properties for the sphere and torus
		// ambient, diffuse, and specular coefficients
		// specular exponent is a global variable
		ColorType torus_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType sphere_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType ellip_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);
		ColorType super_ka = new ColorType(1.0f*factorA,1.0f*factorA,1.0f*factorA);

		ColorType torus_kd = new ColorType(0.0f*factorD,0.5f*factorD,0.9f*factorD);
		ColorType sphere_kd = new ColorType(0.9f*factorD,0.3f*factorD,0.1f*factorD);
		ColorType ellip_kd = new ColorType(0.1f*factorD,0.9f*factorD,0.01f*factorD);
		ColorType super_kd = new ColorType(1.0f*factorD,1.0f*factorD,1.0f*factorD);

		ColorType torus_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType sphere_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType ellip_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);
		ColorType super_ks = new ColorType(1.0f*factorS,1.0f*factorS,1.0f*factorS);

		// material properties stored in array
		Material[] mats = {
			new Material(matTerms, sphere_ka, sphere_kd, sphere_ks, ns),
			new Material(matTerms, torus_ka, torus_kd, torus_ks, ns),
			new Material(matTerms, ellip_ka, ellip_kd, ellip_ks, ns),
			new Material(matTerms, super_ka, super_kd, super_ks, ns)};


		// if we have not already made the lights for this scene
		if(!lightMade) {

			lightMade = true;

			lights = new ArrayList<Light>();

			// define one infinite light source, with color = white
			ColorType light_color = new ColorType(1.0f,1.0f,1.0f);
			Vector3D light_direction = new Vector3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
			Light light = new Light(light_color,light_direction, false, true);

			lights.add(light);

			// define one ambient light source, with color = white
			light_color = new ColorType(1.0f,0.0f,0.0f);
			light_direction = new Vector3D((float)0.0,(float)(-1.0/Math.sqrt(2.0)),(float)(1.0/Math.sqrt(2.0)));
			Light ambient = new Light(light_color,light_direction, true, false);

			lights.add(ambient);

			// point light with color = red
			light_color = new ColorType(1.0f,0.3f,1.0f);
			light_direction = new Vector3D(0f, -1f, 1f);
			Vector3D light_position = new Vector3D(100f, 100f, 200f);
			Light spot = new Light(light_color,light_direction, light_position);

			lights.add(spot);

		}
		
		// normal to the plane of a triangle
		// to be used in backface culling / backface rejection
		Vector3D triangle_normal = new Vector3D();
		
		// a triangle mesh
		Mesh3D mesh;
			
		int i, j, n, m;
		
		// temporary variables for triangle 3D vertices and 3D normals
		Vector3D v0,v1, v2, n0, n1, n2;
		
		// projected triangle, with vertex colors
		Point3D[] tri = {new Point3D(), new Point3D(), new Point3D()};
		
		for(int k=0;k<4;++k) // loop through all objects in scene
		{
			if(k == 0)
			{
				mesh=sphere.mesh;
				n=sphere.get_n();
				m=sphere.get_m();
			}
			else if(k == 1)
			{
				mesh=torus.mesh;
				n=torus.get_n();
				m=torus.get_m();
			}
			else if(k == 2)
			{
				mesh = ellip.mesh;
				n = ellip.get_n();
				m = ellip.get_m();
			}
			else
			{
				mesh = superEllip.mesh;
				n = superEllip.get_n();
				m = superEllip.get_m();
			}

			
			// rotate the surface's 3D mesh using quaternion
			mesh.rotateMesh(viewing_quaternion, viewing_center);
					
			// draw triangles for the current surface, using vertex colors
			// this works for Gouraud and flat shading only (not Phong)
			for(i=0; i < m-1; ++i)
			{
				for(j=0; j < n-1; ++j)
				{
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j+1];
					v2 = mesh.v[i+1][j+1];
					triangle_normal = computeTriangleNormal(v0,v1,v2);
					
					if(view_vector.dotProduct(triangle_normal) > 0.0)  // front-facing triangle?
					{	
						if(doSmoothShading)  
						{
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j+1];
							n2 = mesh.n[i+1][j+1];
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}
						else 
						{
							// flat shading: use the normal to the triangle itself
							n2 = n1 = n0 =  triangle_normal;
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}

						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[0].z = (int)v0.z;

						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[1].z = (int)v1.z;

						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						tri[2].z = (int)v2.z;
	
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],doSmoothShading,depthBuffer);      
					}
					
					v0 = mesh.v[i][j];
					v1 = mesh.v[i+1][j+1];
					v2 = mesh.v[i+1][j];
					triangle_normal = computeTriangleNormal(v0,v1,v2);
					
					if(view_vector.dotProduct(triangle_normal) > 0.0)  // front-facing triangle?
					{	
						if(doSmoothShading)
						{
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i+1][j+1];
							n2 = mesh.n[i+1][j];
							tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
							tri[1].c = applyAllLights(lights, mats[k], view_vector, n1, v1);
							tri[2].c = applyAllLights(lights, mats[k], view_vector, n2, v2);
						}
						else 
						{
							// flat shading: use the normal to the triangle itself
							n2 = n1 = n0 =  triangle_normal;
							tri[2].c = tri[1].c = tri[0].c = applyAllLights(lights, mats[k], view_vector, n0, v0);
						}	
			
						tri[0].x = (int)v0.x;
						tri[0].y = (int)v0.y;
						tri[1].x = (int)v1.x;
						tri[1].y = (int)v1.y;
						tri[2].x = (int)v2.x;
						tri[2].y = (int)v2.y;
						
						SketchBase.drawTriangle(buff,tri[0],tri[1],tri[2],doSmoothShading,depthBuffer);      
					}
				}	
			}
		}
	}

	// helper method that computes the unit normal to the plane of the triangle
	// degenerate triangles yield normal that is numerically zero
	private Vector3D computeTriangleNormal(Vector3D v0, Vector3D v1, Vector3D v2)
	{
		Vector3D e0 = v1.minus(v2);
		Vector3D e1 = v0.minus(v2);
		Vector3D norm = e0.crossProduct(e1);
		
		if(norm.magnitude()>0.000001)
			norm.normalize();
		else 	// detect degenerate triangle and set its normal to zero
			norm.set((float)0.0,(float)0.0,(float)0.0);

		return norm;
	}

}