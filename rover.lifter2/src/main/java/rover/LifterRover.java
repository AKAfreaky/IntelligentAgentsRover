package rover;

public class LifterRover extends MessagingRover
{
	public LifterRover()
	{
		MAX_SPEED	= 4;
		MAX_SCAN	= 0;
		CAPACITY	= 5;
		
		roverType	= 2;

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
