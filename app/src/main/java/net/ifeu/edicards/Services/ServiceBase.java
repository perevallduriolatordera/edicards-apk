package net.ifeu.edicards.Services;

public class ServiceBase {

	private final ServiceMonitor _monitor;
	
	public ServiceBase() {
		this._monitor = new ServiceMonitor();
	}
	
	public ServiceMonitor Monitor() {
		return this._monitor;
	}
}
