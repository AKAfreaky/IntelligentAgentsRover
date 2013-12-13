package rover;

public class SoloRover extends MessagingRover
{
	public SoloRover()
	{
		MAX_SPEED	= 3;
		MAX_SCAN	= 3;
		CAPACITY	= 3;
		
		roverType	= 3;

		try 
		{
			// Set attributes for this rover
			// speed, scan range, max load have to add up to <= 9
			// Check because I can be stupid.
			if ( ( MAX_SPEED + MAX_SCAN + CAPACITY ) <= 9)
			{
				setAttributes( MAX_SPEED , MAX_SCAN , CAPACITY );
			}
			else
			{
				setAttributes(4, 4, 1);
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		init();
	}
	
}