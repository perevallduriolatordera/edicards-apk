package net.ifeu.edicards;

import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.edicards.DataTier.TransferMode;
import net.ifeu.edicards.Services.ServiceMonitor;

public class WorkingArea {

	public Cliente CurrentCliente;
	public Articulo CurrentArticulo;
	public Deposito CurrentDeposito;
	public Reporting CurrentReporting;
	public boolean CancelSearchDeposit;
	public Historico CurrentHistorico;
	public boolean UpgradeDataPost;
	public TransferMode TransferMode;
	public Deposito InitialDeposito;
	public ServiceMonitor Monitor;
	public DepositoModalidad CurrentDepositoModalidad;
	
	public WorkingArea()
	{
		this.TransferMode = net.ifeu.edicards.DataTier.TransferMode.None;
	}
	
}




