package rover;

public class CompetitiveLifterRover extends MessagingRover
{
	public CompetitiveLifterRover()
	{
		MAX_SPEED	= 7;
		MAX_SCAN	= 0;
		CAPACITY	= 2;
		
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
