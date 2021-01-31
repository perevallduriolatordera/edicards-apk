package net.ifeu.edicards.DataTier.Support;

public class ValidationResult {

	public boolean Result;
	public String MessageResult;
	
	public ValidationResult(boolean result, String messageResult)
	{
		this.Result = result;
		this.MessageResult = messageResult;
	}
}
