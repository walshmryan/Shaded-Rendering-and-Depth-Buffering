//****************************************************************************
//      Superellipsoid class
//****************************************************************************
// History :
//   Dec 2016 Edited by Ryan Walsh for PA4
//   Nov 6, 2014 Created by Stan Sclaroff
//

public class Superellipsoid
{
	private Vector3D center;
	private float rx, ry, rz;
	private int m,n;
	private double e;
	public Mesh3D mesh;
	
	public Superellipsoid(float _x, float _y, float _z, float _rx, float _ry, float _rz, int _m, int _n, double _e)
	{
		center = new Vector3D(_x,_y,_z);
		rx = _rx;
		ry = _ry;
		rz = _rz;
		m = _m;
		n = _n;
		e = _e;
		initMesh();
	}
	
	public void set_center(float _x, float _y, float _z)
	{
		center.x=_x;
		center.y=_y;
		center.z=_z;
		fillMesh();  // update the triangle mesh
	}
	
	public void set_radius(float _rx, float _ry, float _rz)
	{
		rx = _rx;
		ry = _ry;
		rz = _rz;
		fillMesh(); // update the triangle mesh
	}
	
	public void set_m(int _m)
	{
		m = _m;
		initMesh(); // resized the mesh, must re-initialize
	}
	
	public void set_n(int _n)
	{
		n = _n;
		initMesh(); // resized the mesh, must re-initialize
	}
	
	public int get_n()
	{
		return n;
	}
	
	public int get_m()
	{
		return m;
	}

	private void initMesh()
	{
		mesh = new Mesh3D(m,n);
		fillMesh();  // set the mesh vertices and normals
	}
		
	// fill the triangle mesh vertices and normals
	// using the current parameters for the sphere
	private void fillMesh()
	{
		// contrainsts for building superellip
		float vmin = (float) -Math.PI;
		float vmax = (float) Math.PI;

		float umin = (float) -Math.PI/2;
		float umax = (float) Math.PI/2;

		// intensities for superellip
		double e1, e2;
		e1 = e2 = e;

		int i,j;		
		float theta, phi;
		float d_theta = vmax * 2 / ((float) m-1);
		float d_phi = umax * 2 / ((float) n-1);

		float c_theta,s_theta;
		float c_phi, s_phi;

		// v = theta
		for(i = 0, theta = vmin;i < m; ++i, theta += d_theta)
	    {
			c_theta = (float)Math.cos(theta);
			s_theta = (float)Math.sin(theta);
			
			// u = phi
			for(j = 0, phi = umin; j < n ; ++j, phi += d_phi)
			{
				// vertex location
				c_phi = (float)Math.cos(phi);
				s_phi = (float)Math.sin(phi);

				// calculate vertex for x,y,z
				mesh.v[i][j].x = center.x + rx * (float)Math.signum(c_phi) * (float)Math.pow(Math.abs(c_phi), e1) * (float)Math.signum(c_theta) * (float)Math.pow(Math.abs(c_theta), e2);
				mesh.v[i][j].y = center.y + ry * (float)Math.signum(c_phi) * (float)Math.pow(Math.abs(c_phi), e1) * (float)Math.signum(s_theta) * (float)Math.pow(Math.abs(s_theta), e2);
				mesh.v[i][j].z = center.z + rz * (float)Math.signum(s_phi) * (float)Math.pow(Math.abs(s_phi), e1);
				
				// unit normal to sphere at this vertex
				mesh.n[i][j].x = c_phi*c_theta;
				mesh.n[i][j].y = c_phi*s_theta;
				mesh.n[i][j].z = s_phi;
			}
	    }
	}
}