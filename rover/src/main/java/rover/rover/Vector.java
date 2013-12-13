package rover;

import org.iids.aos.log.Log;

public class Vector extends ImmutableVector
{

	public Vector(double x, double y)
	{
		super(x, y);
	}
	
	
	public Vector()
	{
		super(0.0f, 0.0f);
	}
	
	
	public Vector( ImmutableVector other )
	{
		super( other );
	}
	
	
	public void setX(double newVal)
	{
		this.x = newVal;
	}
	
	
	public void setY(double newVal)
	{
		this.y = newVal;
	}
	
	
	public void setXLongBits(long longBits)
	{
		this.x = Double.longBitsToDouble( longBits );
	}
	
	
	public void setYLongBits(long longBits)
	{
		this.y = Double.longBitsToDouble( longBits );
	}
		
}