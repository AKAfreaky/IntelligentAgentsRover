package rover;

import java.util.HashSet;
import org.iids.aos.log.Log;

public class WorldMap
{
	private HashSet<ImmutableVector> resources; // resource nodes discovered
	
	private HashSet<ImmutableVector> activeResources; // resource nodes being collected from
	
	private HashSet<ImmutableVector> bases; // other bases discovered
	
	private ImmutableVector worldSize;
	
	private Vector currPos;
	
	public WorldMap()
	{
		worldSize 		= new ImmutableVector(0.0f, 0.0f);
		currPos 		= new Vector();
		resources 		= new HashSet<ImmutableVector>();
		activeResources	= new HashSet<ImmutableVector>();
		bases			= new HashSet<ImmutableVector>();
		bases.add(new ImmutableVector(0.0f, 0.0f)); // We consider the base position 0,0
	}
	
	public WorldMap( double sizeX, double sizeY )
	{
		worldSize 		= new ImmutableVector( sizeX, sizeY );	
		currPos 		= new Vector();
		resources 		= new HashSet<ImmutableVector>();
		activeResources	= new HashSet<ImmutableVector>();
		bases			= new HashSet<ImmutableVector>();
		bases.add(new ImmutableVector(0.0f, 0.0f)); // We consider the base position 0,0
		
	}
	
	public synchronized void init( double sizeX, double sizeY )
	{
		worldSize 	= new ImmutableVector( sizeX, sizeY );
	}
	
		
	public double normaliseToValue( double input, double value )
	{
		double output = input;
		while( output < 0 )
			output += value;
			
		output = output % value;
		
		return output;
	}
	
	
	public synchronized void updatePosistion( double offsetX, double offsetY )
	{
		currPos.setX( currPos.getX() + offsetX );
		currPos.setY( currPos.getY() + offsetY );
					
		currPos.setX( normaliseToValue( currPos.getX(), worldSize.getX()) );
		currPos.setY( normaliseToValue( currPos.getY(), worldSize.getY()) );
	}
	
	
	public synchronized ImmutableVector getCurrPosition()
	{
		return new ImmutableVector(currPos);
	}
	
	
	public synchronized void addResource( double offsetX, double offsetY )
	{
		resources.add(new ImmutableVector( normaliseToValue( currPos.getX() + offsetX, worldSize.getX()),
										   normaliseToValue( currPos.getY() + offsetY, worldSize.getY())));
	}
	
	
	public synchronized void addResourceAbs( double xPos, double yPos )
	{
		resources.add(new ImmutableVector( normaliseToValue( xPos, worldSize.getX()),
										   normaliseToValue( yPos, worldSize.getY())));
	}
	
	public synchronized void addActiveResource( double offsetX, double offsetY )
	{
		activeResources.add(new ImmutableVector( normaliseToValue( currPos.getX() + offsetX, worldSize.getX()),
												 normaliseToValue( currPos.getY() + offsetY, worldSize.getY())));
	}
	
	
	public synchronized void addActiveResourceAbs( double xPos, double yPos )
	{
		activeResources.add(new ImmutableVector( normaliseToValue( xPos, worldSize.getX()),
												 normaliseToValue( yPos, worldSize.getY())));
	}
	
	public synchronized void activateResource( ImmutableVector resource )
	{
		activeResources.add(resource);
		resources.remove(resource);
	}
	
	
	public synchronized void removeResource( ImmutableVector resource )
	{
		resources.remove(resource);
		activeResources.remove(resource);
	}
	
	
	public synchronized int numResourceFound()
	{
		return resources.size();
	}
	
	public synchronized int numActiveResources()
	{
		return activeResources.size();
	}
	
	
	public synchronized void addBase( double offsetX, double offsetY )
	{
		bases.add(new ImmutableVector(currPos.getX() + offsetX,
									  currPos.getY() + offsetY));
	}
	
	public synchronized ImmutableVector closestResource()
	{
		double minDist 			= 999999.0f;
		ImmutableVector closest = null;
		for(ImmutableVector res : resources)
		{
			double dist = distanceToPosition( res.getX(), res.getY() );
			
			if( dist < minDist )
			{
				minDist = dist;
				closest = res;
			}
		}
		
		return closest;
	}
	
	
	
	// The offset to move to a position
	public synchronized ImmutableVector offsetToPosition( double posX, double posY )
	{
		double dxL, dxR, dyL, dyR, normX, normY;
		
		normX = normaliseToValue( posX, worldSize.getX() );
		normY = normaliseToValue( posY, worldSize.getY() );
		
		dxL = (normX < currPos.getX()) ? normX - currPos.getX() : (normX - worldSize.getX()) - currPos.getX();
		dxR = (normX < currPos.getX()) ? (worldSize.getX() - currPos.getX()) + normX : normX - currPos.getX();
		
		dyL = (normY < currPos.getY()) ? normY - currPos.getY() : (normY - worldSize.getY()) - currPos.getY();
		dyR = (normY < currPos.getY()) ? (worldSize.getY() - currPos.getY()) + normX : normY - currPos.getY();
		
		
	
		return new ImmutableVector( ( Math.abs(dxL) < Math.abs(dxR) ) ? dxL : dxR ,
									( Math.abs(dyL) < Math.abs(dyR) ) ? dyL : dyR );
	}
	
	
	public synchronized double distanceToPosition( double posX, double posY )
	{
		double diffX = Math.abs( currPos.getX() - posX );
		double diffY = Math.abs( currPos.getY() - posY );
	
		return Math.sqrt( Math.pow(Math.min( diffX, worldSize.getX() - diffX ), 2) +
						  Math.pow(Math.min( diffY, worldSize.getY() - diffY ), 2));
	}
	
	
	// Convenience method 
	public ImmutableVector offsetToHome()
	{
		return offsetToPosition( 0.0f, 0.0f);
	}
	
	
	// Close enough to deposit (and I assume collect). That's within .1
	public synchronized boolean closeTo(ImmutableVector other)
	{
		ImmutableVector offset = offsetToPosition( other.getX(), other.getY() );
		return ( Math.abs(offset.getX()) < 0.1f && Math.abs(offset.getY()) <0.1f);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}