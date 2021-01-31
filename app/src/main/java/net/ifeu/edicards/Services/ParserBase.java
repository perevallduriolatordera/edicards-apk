package net.ifeu.edicards.Services;

public class ParserBase {

	protected ParserMonitor _monitor;
	
	public ParserBase() {
		this._monitor = new ParserMonitor();
	}
	
	public ParserMonitor Monitor() {
		return this._monitor;
	}
}
