package rover;

// Structure of this class came from this SO post: http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java


public class ImmutableVector 
{
    protected double x;
    protected double y;

    public ImmutableVector(double x, double y)
	{
    	this.x = x;
    	this.y = y;
    }
	
	
	// Copy constructor
	public ImmutableVector(ImmutableVector other)
	{
		this.x = other.getX();
		this.y = other.getY();
	}

	
    public int hashCode()
	{
    	int hashX 	= Double.valueOf(x).hashCode();
    	int hashY	= Double.valueOf(y).hashCode();

    	return (hashX + hashY) * hashX + hashY;
    }

	
    public boolean equals(Object other) 
	{
    	if (other instanceof ImmutableVector) 
		{
    		ImmutableVector otherVector = (ImmutableVector) other;
    		return ( (this.x == otherVector.x) && (this.y == otherVector.y) );
    	}

    	return false;
    }
	
	
	public double getX()
	{
		return x;
	}
	
	
	public double getY()
	{
		return y;
	}
	
	public long getXLongBits()
	{
		return Double.doubleToRawLongBits( x );
	}
	
	public long getYLongBits()
	{
		return Double.doubleToRawLongBits( y );
	}
	
	
	
}