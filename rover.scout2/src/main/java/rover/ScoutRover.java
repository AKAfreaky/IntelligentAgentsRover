package rover;

public class ScoutRover extends MessagingRover
{
	public ScoutRover()
	{
		MAX_SPEED	= 5;
		MAX_SCAN	= 4;
		CAPACITY	= 0;
		
		roverType	= 1;

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