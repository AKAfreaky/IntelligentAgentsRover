package rover;

import java.io.Serializable;

public class Message implements java.io.Serializable
{
	private MsgType msgType;
	private String  msgData;
	private int		targetHash;
	
	public Message()
	{
		msgType 	= MsgType.NONE;
		msgData 	= "NULL";
		targetHash	= 0;
	}
	
	public Message( MsgType type, String data, int hash )
	{
		msgType 	= type;
		msgData 	= data;
		targetHash 	= hash;
	}
	
	public MsgType getType()
	{
		return msgType;
	}
	
	public void setType( MsgType type )
	{
		msgType = type;
	}
	
	public String getData()
	{
		return msgData;
	}
	
	public void setData( String data )
	{
		msgData = data;
	}
	
	public int getTarget()
	{
		return targetHash;
	}
	
	public void setTarget( int hash )
	{
		targetHash = hash;
	}
	
}