package net.ifeu.edicards.DataTier;

import net.ifeu.edicards.DataTier.Support.IDataTierBase;
import net.ifeu.edicards.DataTier.Support.ValidationResult;

public class User implements IDataTierBase {

	public String User;
	public String Password;
	public String SerialInvoiceA;
	public String SerialInvoiceB;
	public String Company;
	public String Name;
	public String InitSerieA;
	public String InitSerieB;
	
	private static final String EMPTY_STRING = "";
		
	public boolean isEmpty()
	{
		return this.User.equals(EMPTY_STRING);
	}
	
	@Override
	public ValidationResult Validate()
	{
		if (this.isEmpty()) {
			return new ValidationResult(false, "No se ha definido un nombre de usuario"); 
		}
		else if (this.SerialInvoiceA.equals(EMPTY_STRING)) { 
			return new ValidationResult(false, "No se ha definido una série A de facturación"); 
		}
		else if (this.SerialInvoiceB.equals(EMPTY_STRING)) {
			return new ValidationResult(false, "No se ha definido una série B de facturación"); }
		else if (this.Company.equals(EMPTY_STRING)) {
			return new ValidationResult(false, "No se ha definido un código de empresa"); }
		else if (this.Name.equals(EMPTY_STRING)) {
			return new ValidationResult(false, "No se ha definido un nombre de empleado"); }
		else {
			return new ValidationResult(true,EMPTY_STRING);
		}
		
			
	}
}
