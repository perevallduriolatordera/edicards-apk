package net.ifeu.edicards;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Utils.MessageBoxType;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class MainMenuFragments extends Fragment implements OnTabChangeListener {

	private static final String TAG = "FragmentTabs";
	public static final String TAB_ALBARANES = "albaranes";
	public static final String TAB_DIETAS = "dietas";
	public static final String TAB_ALBARANES_CAPTION = "Gestión de albaranes";
	public static final String TAB_DIETAS_CAPTION = "Gestión de dietas";
	public static final String TAB_INFORMES = "informes";
	public static final String TAB_INFORMES_CAPTION = "Informes";
	public static final String TAB_CLIENTES = "clientes";
	public static final String TAB_CLIENTES_CAPTION = "Clientes";
	public static final String TAB_ARTICULOS = "articulos";
	public static final String TAB_ARTICULOS_CAPTION = "Articulos";
	public static final String TAB_STOCK = "stock";
	public static final String TAB_STOCK_CAPTION = "Almacén";
	public static final String TAB_DEPOSITOS = "deposito";
	public static final String TAB_DEPOSITOS_CAPTION = "Depósito";
	public static final String TAB_CLOSE = "cerrar";
	public static final String TAB_CLOSE_CAPTION = "Cerrar";
	public static final String TAB_NOTIFICACIONES = "notificaciones";
	public static final String TAB_NOTIFICACIONES_CAPTION = "Notificaciones";
	public static final String TAB_INGRESOS = "ingresos";
	public static final String TAB_INGRESOS_CAPTION = "Ingresos";
	public static final String TAB_SINCRO = "sincro";
	public static final String TAB_SINCRO_CAPTION = "Sincronización";

	private View _root;
	private TabHost _tabHost;
	private int _currentTab;
	private AppConfig _appConfig;

	private Fragment _currentFragment;

	/*private CustomerSearch _customerSearch = new CustomerSearch();
	private ArticleSearch _articleSearch = new ArticleSearch();
	private StockManager _stockManager = new StockManager();*/

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i("MainMenuFragments", "Creem el menú principal");
		_root = inflater.inflate(R.layout.activity_main_menu_fragments, null);
		_tabHost = (TabHost) _root.findViewById(android.R.id.tabhost);
		setupTabs();
		return _root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		
		this._appConfig = (AppConfig) this.getActivity().getApplicationContext();

		_tabHost.setOnTabChangedListener(this);
		_tabHost.setCurrentTab(_currentTab);
		// manually start loading stuff in the first tab
		updateTab(TAB_STOCK, R.id.tab_6);
	}

	private void setupTabs() {
		_tabHost.setup(); // important!

		_tabHost.addTab(newTab(TAB_STOCK, TAB_STOCK_CAPTION, R.id.tab_6));
		_tabHost.addTab(newTab(TAB_DEPOSITOS, TAB_DEPOSITOS_CAPTION, R.id.tab_7));
		_tabHost.addTab(newTab(TAB_INFORMES, TAB_INFORMES_CAPTION, R.id.tab_3));
		_tabHost.addTab(newTab(TAB_DIETAS, TAB_DIETAS_CAPTION, R.id.tab_2));
		_tabHost.addTab(newTab(TAB_INGRESOS, TAB_INGRESOS_CAPTION, R.id.tab_10));
		_tabHost.addTab(newTab(TAB_CLIENTES, TAB_CLIENTES_CAPTION, R.id.tab_4));
		_tabHost.addTab(newTab(TAB_ARTICULOS, TAB_ARTICULOS_CAPTION, R.id.tab_5));
		_tabHost.addTab(newTab(TAB_SINCRO, TAB_SINCRO_CAPTION, R.id.tab_11));
		_tabHost.addTab(newTab(TAB_CLOSE, TAB_CLOSE_CAPTION, R.id.tab_8));

	}

	private TabSpec newTab(String tag, String labelId, int tabContentId) {
		Log.d(TAG, "buildTab(): tag=" + tag);

		View indicator = LayoutInflater.from(getActivity()).inflate(
				R.layout.tab,
				(ViewGroup) _root.findViewById(android.R.id.tabs), false);
		((TextView) indicator.findViewById(R.id.text)).setText(labelId);

		TabSpec tabSpec = _tabHost.newTabSpec(tag);
		tabSpec.setIndicator(indicator);
		tabSpec.setContent(tabContentId);
		return tabSpec;
	}

	public void onTabChanged(String tabId) {
		Log.d(TAG, "onTabChanged(): tabId=" + tabId);

		if (TAB_INFORMES.equals(tabId)) {
			updateTab(tabId, R.id.tab_3);
			_currentTab = 2;
			return;
		}

		if (TAB_DIETAS.equals(tabId)) {
			updateTab(tabId, R.id.tab_2);
			_currentTab = 1;
			return;
		}
		if (TAB_CLIENTES.equals(tabId)) {
			updateTab(tabId, R.id.tab_4);
			_currentTab = 3;
			return;
		}
		if (TAB_ARTICULOS.equals(tabId)) {
			updateTab(tabId, R.id.tab_5);
			_currentTab = 4;
			return;
		}
		if (TAB_STOCK.equals(tabId)) {
			updateTab(tabId, R.id.tab_6);
			_currentTab = 5;
			return;
		}
		if (TAB_DEPOSITOS.equals(tabId)) {
			
			try {
				if (this.RestriccionIngresos()) {
					_appConfig.getMessageBox()
					.Show("Atención",
							"Ha superado los " + Constants.MAXIMO_SIN_INGRESAR + " € pendientes de ingresar. Realice un ingreso para poder seguir trabajando",
							getActivity(), MessageBoxType.Error);
					
					updateTab(TAB_INGRESOS, R.id.tab_10);
					_currentTab = 10;
					return;
				} else {
					updateTab(tabId, R.id.tab_7);
					_currentTab = 6;
					return;		
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			}
		}
		if (TAB_CLOSE.equals(tabId)) {
			updateTab(tabId, R.id.tab_8);
			_currentTab = 8;
			return;
		}
		if (TAB_INGRESOS.equals(tabId)) {
			updateTab(tabId, R.id.tab_10);
			_currentTab = 10;
			return;
		}

		if (TAB_NOTIFICACIONES.equals(tabId)) {
			updateTab(tabId, R.id.tab_9);
			_currentTab = 7;
			return;
		}
		
		if (TAB_SINCRO.equals(tabId)) {
			updateTab(tabId, R.id.tab_11);
			_currentTab = 11;
			return;
		}

	}

	private void updateTab(String tabId, int placeholder) {

		try {
			Log.d(TAG, "updateTab(): tabId=" + tabId);
	
			FragmentManager fragmentManager = getFragmentManager();

			if (_currentFragment != null)
				fragmentManager.beginTransaction().remove(_currentFragment).commit();

			if (tabId.equals("clientes")) {
	
				if (fragmentManager.findFragmentByTag(tabId) == null) {

					CustomerSearch customerSearch = new CustomerSearch();
					Log.d(TAG, "BeginTransaction clientes: tabId=" + tabId);
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, customerSearch)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
					_currentFragment = customerSearch;
				}
			} else if (tabId.equals("articulos")) {
	
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction articulos: tabId=" + tabId);
					ArticleSearch articleSearch = new ArticleSearch();
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, articleSearch)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = articleSearch;
				}
			} else if (tabId.equals("dietas")) {
				GastosManager gastos = new GastosManager();
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction dietas: tabId=" + tabId);
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, gastos)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = gastos;
				}
			} else if (tabId.equals("informes")) {
				Reports reports = new Reports();
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction dietas: tabId=" + tabId);
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, reports)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = reports;
				}
			} else if (tabId.equals("stock")) {
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction stock: tabId=" + tabId);

					StockManager stockManager = new StockManager();
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, stockManager)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = stockManager;
				}
			} else if (tabId.equals("deposito")) {
				DepositManager deposito = new DepositManager();
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction deposito: tabId=" + tabId);
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, deposito)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = deposito;
				} 
			} else if (tabId.equals("ingresos")) {
					
					IngresoData ingresos = new IngresoData();
					
					if (fragmentManager.findFragmentByTag(tabId) == null) {
						Log.d(TAG, "BeginTransaction ingresos: tabId=" + tabId);
						fragmentManager
								.beginTransaction()
								.replace(R.id.fragment_placeholder, ingresos)
								.setTransition(
										FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

						_currentFragment = ingresos;
				}
			}
	
			else if (tabId.equals("cerrar")) {
	
				this.getActivity().moveTaskToBack(true);
				System.exit(0);
			} 
			
			else if (tabId.equals("sincro")) {
				
				MonitorView monitor = new MonitorView();
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction monitor: tabId=" + tabId);
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, monitor)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = monitor;
			}
	
			else if (tabId.equals("notificaciones")) {
	
				SenderMessages sender = new SenderMessages();
				if (fragmentManager.findFragmentByTag(tabId) == null) {
					Log.d(TAG, "BeginTransaction sender: tabId=" + tabId);
					fragmentManager
							.beginTransaction()
							.replace(R.id.fragment_placeholder, sender)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();

					_currentFragment = sender;
					}
				}
			}

			System.gc();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e1);
		}
	}
	
	private boolean RestriccionIngresos() throws Exception {

		//double cantidadPagada = this.getCantidadPagada();
		//double ingresos = this.getIngresos();

		// return Constants.MAXIMO_SIN_INGRESAR <= (cantidadPagada - ingresos);
		return false;
	}


}
