package net.ifeu.edicards;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEvents;
import net.ifeu.library.Mediator.IMediator;

public class MainMenuFragments extends Fragment implements OnTabChangeListener, IMediator {

	public static final String TAB_DIETAS = "dietas";
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
	public static final String TAB_SINCRO = "sincro";
	public static final String TAB_SINCRO_CAPTION = "Herramientas";

	private View _root;
	private TabHost _tabHost;
	private int _currentTab;
	private AppConfig _appConfig;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		_root = inflater.inflate(R.layout.activity_main_menu_fragments, null);
		_tabHost = (TabHost) _root.findViewById(android.R.id.tabhost);
		this._appConfig = (AppConfig) this.getActivity().getApplicationContext();

		setupTabs();
		assignToMediator(this, false);
		return _root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);

		_tabHost.setOnTabChangedListener(this);
		_tabHost.setCurrentTab(_currentTab);
		// manually start loading stuff in the first tab
		updateTab(TAB_STOCK, StockManager.class);
	}

	@Override 	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setupTabs() {
		_tabHost.setup(); // important!

		_tabHost.addTab(newTab(TAB_STOCK, TAB_STOCK_CAPTION, R.id.tab_6));
		_tabHost.addTab(newTab(TAB_DEPOSITOS, TAB_DEPOSITOS_CAPTION, R.id.tab_7));
		_tabHost.addTab(newTab(TAB_INFORMES, TAB_INFORMES_CAPTION, R.id.tab_3));
		_tabHost.addTab(newTab(TAB_DIETAS, TAB_DIETAS_CAPTION, R.id.tab_2));
		_tabHost.addTab(newTab(TAB_CLIENTES, TAB_CLIENTES_CAPTION, R.id.tab_4));
		_tabHost.addTab(newTab(TAB_ARTICULOS, TAB_ARTICULOS_CAPTION, R.id.tab_5));
		_tabHost.addTab(newTab(TAB_SINCRO, TAB_SINCRO_CAPTION, R.id.tab_11));
		_tabHost.addTab(newTab(TAB_CLOSE, TAB_CLOSE_CAPTION, R.id.tab_8));

	}

	private TabSpec newTab(String tag, String labelId, int tabContentId) {

		View indicator = LayoutInflater.from(getActivity()).inflate(
				R.layout.tab,
				(ViewGroup) _root.findViewById(android.R.id.tabs), false);
		((TextView) indicator.findViewById(R.id.text)).setText(" " + labelId + " ");

		TabSpec tabSpec = _tabHost.newTabSpec(tag);
		tabSpec.setIndicator(indicator);
		tabSpec.setContent(tabContentId);
		return tabSpec;
	}

	public void onTabChanged(String tabId) {

		if (TAB_INFORMES.equals(tabId)) {
			updateTab(tabId, Reports.class);
			_currentTab = 2;
			return;
		}

		if (TAB_DIETAS.equals(tabId)) {
			updateTab(tabId, GastosManager.class);
			_currentTab = 1;
			return;
		}
		if (TAB_CLIENTES.equals(tabId)) {
			updateTab(tabId, CustomerSearch.class);
			_currentTab = 3;
			return;
		}
		if (TAB_ARTICULOS.equals(tabId)) {
			updateTab(tabId, ArticleSearch.class);
			_currentTab = 4;
			return;
		}
		if (TAB_STOCK.equals(tabId)) {
			updateTab(tabId, StockManager.class);
			_currentTab = 5;

			return;
		}
		if (TAB_DEPOSITOS.equals(tabId)) {

			try {
				updateTab(tabId, DepositManager.class);
				_currentTab = 6;

				return;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		if (TAB_CLOSE.equals(tabId)) {
			updateTab(tabId, null);
			_currentTab = 8;
			return;
		}

		if (TAB_SINCRO.equals(tabId)) {
			updateTab(tabId, MonitorView.class);
			_currentTab = 11;
		}

	}

	private void updateTab(String tabId, Class fragmentType, boolean ...forceRecreated) {

		if (fragmentType == null) {
			getActivity().finish();
			System.exit(0);
		}

		FragmentManager fragmentManager = getFragmentManager();

		Fragment fragment = fragmentManager.findFragmentByTag(tabId);

		if (forceRecreated.length > 0 && forceRecreated[0] && fragment != null) {
			FragmentTransaction trans = fragmentManager.beginTransaction();
			trans.remove(fragment);
			trans.commit();
		}

		if (fragment == null) {

			try {
				fragment = (Fragment) fragmentType.newInstance();

			} catch (java.lang.InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		fragmentManager
				.beginTransaction()
				.replace(R.id.fragment_placeholder, fragment, tabId)
				.addToBackStack(null)
				.setTransition(
						FragmentTransaction.TRANSIT_FRAGMENT_FADE).commitAllowingStateLoss();

		if (fragment instanceof IMediator)
			assignToMediator((IMediator) fragment, forceRecreated.length > 0 && forceRecreated[0]);

		System.gc();

	}

	private void assignToMediator(IMediator mediator, boolean force) {
		_appConfig.getMediator().addReceiver(mediator, force);
	}

	@Override
	public void notify(String event, Object payload) {
		if (event == ConstantsEvents.EVENT_DEPOSIT_CLOSED) {
			onTabChanged(TAB_DEPOSITOS);
		}

	}
}
