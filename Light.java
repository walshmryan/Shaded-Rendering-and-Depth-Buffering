//****************************************************************************
//       Infinite light source class
//****************************************************************************
// History :
//   Dec 2016 Edited by Ryan Walsh for PA4
//   Nov 6, 2014 Created by Stan Sclaroff
//
public class Light 
{
	public Vector3D direction;
	public Vector3D position;
	public ColorType color;

	public boolean ambient;
	public boolean infinite;

	public float a0, a1, a2;
	public int angle;

	public boolean isOn;
	
	// constructor for ambient and infinite light models
	public Light(ColorType _c, Vector3D _direction, boolean _ambient, boolean _infinite)
	{
		color = new ColorType(_c);
		direction = new Vector3D(_direction);

		infinite = _infinite;
		ambient = _ambient;

		isOn = true;
	}

	// constructor for point light models
	public Light(ColorType _c, Vector3D _direction, Vector3D _position)
	{
		color = new ColorType(_c);
		direction = new Vector3D(_direction);
		position = new Vector3D(_position);

		infinite = false;
		ambient = false;

		a0 = a1 = a2 = 0.00001f;
		angle = 40;

		isOn = true;
		
	}

	public void changeState()
	{
		isOn = !isOn;

		if(isOn)
		{
			System.out.println("Light turned on");
		} else 
		{
			System.out.println("Light turned off");
		}
	}
	
	// apply this light source to the vertex / normal, given material
	// return resulting color value
	public ColorType applyLight(Material mat, Vector3D v, Vector3D n, Vector3D ps){
		
		ColorType res = new ColorType();
		
		// dot product between light direction and normal
		// light must be facing in the positive direction
		// dot <= 0.0 implies this light is facing away (not toward) this point
		// therefore, light only contributes if dot > 0.0
		double dot;
		Vector3D pl = new Vector3D(1, 1, 1);

		// compute dot based on which light source it is
		if(infinite || ambient)
		{
			dot = direction.dotProduct(n);

		} else {

			pl = position.minus(ps);
			pl.normalize();
			dot = pl.dotProduct(n);
		}

		// if point is reachable
		if(dot>0.0)
		{

			// ambient component if it is an ambient light source
			if(mat.ambient && ambient)
			{
				res.r = (float) (color.r * mat.ka.r);
				res.g = (float) (color.g * mat.ka.g);
				res.b = (float) (color.b * mat.ka.b);
			}

			// diffuse component
			if(mat.diffuse)
			{
				res.r = (float)(dot*mat.kd.r*color.r);
				res.g = (float)(dot*mat.kd.g*color.g);
				res.b = (float)(dot*mat.kd.b*color.b);
			}
			// specular component
			if(mat.specular)
			{
				Vector3D r = direction.reflect(n);
				dot = r.dotProduct(v);
				if(dot>0.0)
				{
					res.r += (float)Math.pow((dot*mat.ks.r*color.r),mat.ns);
					res.g += (float)Math.pow((dot*mat.ks.g*color.g),mat.ns);
					res.b += (float)Math.pow((dot*mat.ks.b*color.b),mat.ns);
				}
			}
	
			// radial + angular attenuation
			if(!infinite && !ambient) {

				// radial
				float d = position.distance(ps);

				float rad = 1 / (a0 + a1 * d + a2 * (float) Math.pow(d, 2));


				// angular
				float cos = pl.dotProduct(direction);

				float ang = 1f;

				if(cos > Math.cos(Math.toRadians(angle)))
				{
					ang = (float) Math.pow(cos, 2);
				}

				res.r *= rad * ang;
				res.g *= rad * ang;
				res.b *= rad * ang;

			}

			// clamp so that allowable maximum illumination level is not exceeded
			res.r = (float) Math.min(1.0, res.r);
			res.g = (float) Math.min(1.0, res.g);
			res.b = (float) Math.min(1.0, res.b);
		}
		return(res);
	}
}
