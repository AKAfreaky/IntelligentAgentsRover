package rover;

public class FastLightLifterRover extends MessagingRover
{
	public FastLightLifterRover()
	{
		MAX_SPEED	= 8;
		MAX_SCAN	= 0;
		CAPACITY	= 1;
		
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
