package net.ifeu.edicards.Application;

import android.widget.LinearLayout;

import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.IngresoDiario;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.edicards.DataTier.TransactionMetadata;
import net.ifeu.edicards.DataTier.TransferMode;
import net.ifeu.edicards.Services.ServiceMonitor;

public class WorkingArea {

	public Cliente CurrentCliente;
	public Articulo CurrentArticulo;
	public Deposito CurrentDeposito;
	public Reporting CurrentReporting;
	public Historico CurrentHistorico;
	public boolean UpgradeDataPost;
	public TransferMode TransferMode;
	public ServiceMonitor Monitor;
	public DepositoModalidad CurrentDepositoModalidad;
	public IngresoDiario CurrentIngresoDiario;
	public boolean IsIngresoDiarioVoluntario;

	public TransactionMetadata CurrentTransactionMetadata;

	public WorkingArea()
	{
		this.TransferMode = net.ifeu.edicards.DataTier.TransferMode.None;
	}

	public void invalidateDepositData() {
		this.CurrentHistorico = null;
		this.CurrentDeposito = null;
		this.CurrentCliente = null;
		this.CurrentArticulo = null;
		this.CurrentTransactionMetadata = null;
		this.CurrentIngresoDiario = null;
	}
	
}




