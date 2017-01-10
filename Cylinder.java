//****************************************************************************
//      Sphere class
//****************************************************************************
// History :
//   Dec 2016 Edited by Ryan Walsh for PA4
//   Nov 6, 2014 Created by Stan Sclaroff
//

public class Cylinder
{
	private Vector3D center;
	private float rx, ry;
	private float umin, umax;
	private int m,n;
	public Mesh3D mesh;
	
	public Cylinder(float _x, float _y, float _z, float _rx, float _ry, float _u, int _m, int _n)
	{
		center = new Vector3D(_x,_y,_z);
		rx = _rx;
		ry = _ry;
		umin = -_u;
		umax = _u;
		m = _m;
		n = _n;
		initMesh();
	}
	
	public void set_center(float _x, float _y, float _z)
	{
		center.x=_x;
		center.y=_y;
		center.z=_z;
		fillMesh();  // update the triangle mesh
	}
	
	public void set_radius(float _rx, float _ry)
	{
		rx = _rx;
		ry = _ry;
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
		int i,j;		
		float theta, phi;
		float d_theta= (float)(Math.PI * 2)/((float)m-1);
		float d_phi=(float)(umax * 2) / ((float)n-1);
		float c_theta,s_theta;
		
		// v
		for(i = 0, theta = (float)-Math.PI; i < m; i++, theta += d_theta)
	    {
			c_theta=(float)Math.cos(theta);
			s_theta=(float)Math.sin(theta);
			
			// u
			for(j = 0, phi = umin; j < n; j++, phi += d_phi)
			{
				// set mesh vertices
				mesh.v[i][j].x = center.x + rx * c_theta;
				mesh.v[i][j].y = center.y + ry * s_theta;
				mesh.v[i][j].z = center.z + phi;
				
				// derivate with respect to u and v
				Vector3D u = new Vector3D(rx * -s_theta, ry * c_theta, 0);
				Vector3D v = new Vector3D(0f, 0f, 1f);

				Vector3D cross = u.crossProduct(v);

				// set normals of mesh
				mesh.n[i][j].x = cross.x;
				mesh.n[i][j].y = cross.y;
				mesh.n[i][j].z = cross.z;

				mesh.n[i][j].normalize();
			}
	    }

	    // caps for end of cylinder
	    for (i = 0, theta = (float)-Math.PI; i < m; i++, theta += d_theta)
	    {
			mesh.n[i][0] = new Vector3D(0, 0, -1);
			mesh.n[i][n-1] = new Vector3D(0, 0, 1);

			mesh.v[i][0] = new Vector3D(center.x, center.y, center.z + umin);
			mesh.v[i][n-1] = new Vector3D(center.x, center.y, center.z + umax);
		}
	}
}