package com.fhc25.percepcion.osiris.mapviewer.ui.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.fhc25.percepcion.osiris.mapviewer.R;
import com.fhc25.percepcion.osiris.mapviewer.manager.ApplicationManager;
import com.fhc25.percepcion.osiris.mapviewer.manager.IApplicationManagerProvider;
import com.fhc25.percepcion.osiris.mapviewer.model.location.MetaData;
import com.fhc25.percepcion.osiris.mapviewer.model.states.api.IInternalStateManager;
import com.fhc25.percepcion.osiris.mapviewer.ui.fragments.indoor.IOsirisMapsforgeIndoorFragment;
import com.fhc25.percepcion.osiris.mapviewer.ui.fragments.indoor.OsirisMapsforgeIndoorFragment;

public class BasicMapViewerActivity extends Activity implements IMapsforgeFragmentProvider {

    private ApplicationManager applicationManager;
    private IInternalStateManager internalStateManager;

    private MapActivityStartupManager mapActivityStartupManager;

    private OsirisMapsforgeIndoorFragment mapsforgeIndoorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_fragment_layout);

        IApplicationManagerProvider applicationManagerProvider = (IApplicationManagerProvider) getApplication();
        applicationManager = applicationManagerProvider.getApplicationManager();
        internalStateManager = (IInternalStateManager) getApplication();

        FragmentManager fragmentManager = getFragmentManager();

        if (mapsforgeIndoorFragment == null) {

            mapsforgeIndoorFragment = new OsirisMapsforgeIndoorFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.activity_fragment_linearlayout, mapsforgeIndoorFragment, "fragment_main");
            fragmentTransaction.commit();
        }

        if (savedInstanceState != null) {
            internalStateManager.loadFromBundle(savedInstanceState);
        }

        mapActivityStartupManager = new MapActivityStartupManager(this, applicationManager,
                this, internalStateManager);
    }

    @Override
    public void onResume() {
        super.onResume();

        MetaData persistedMetaData = internalStateManager.persistedDataExists();

        if (persistedMetaData == null) {
            mapActivityStartupManager.startupFromScratch();
        }
        else {
            mapActivityStartupManager.startupFromRestart();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        internalStateManager.persistInternalStateVariable();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        internalStateManager.saveToBundle(savedInstanceState);
    }

    @Override
    public IOsirisMapsforgeIndoorFragment getMapsforgeFragment() {
        return mapsforgeIndoorFragment;
    }
}
