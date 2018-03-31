package tz.co.nezatech.apps.surveytool.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import tz.co.nezatech.apps.surveytool.R;
import tz.co.nezatech.apps.surveytool.db.model.DataType;
import tz.co.nezatech.apps.surveytool.db.model.Form;
import tz.co.nezatech.apps.surveytool.db.model.FormInstance;
import tz.co.nezatech.apps.surveytool.db.model.Setup;

/**
 * Database helper which creates and upgrades the database and provides the DAOs for the app.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "survey.db";
    private static final int DATABASE_VERSION = 20;

    private Dao<Form, Integer> formDao;
    private Dao<FormInstance, Integer> formInstanceDao;
    private Dao<Setup, String> setupDao;
    private Dao<DataType, String> dataTypeDao;
    private Context ctx;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
        ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Form.class);
            TableUtils.createTable(connectionSource, FormInstance.class);
            TableUtils.createTable(connectionSource, Setup.class);
            TableUtils.createTable(connectionSource, DataType.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Unable to create datbases", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqliteDatabase, ConnectionSource connectionSource, int oldVer, int newVer) {
        try {
            TableUtils.dropTable(connectionSource, FormInstance.class, true);
            TableUtils.dropTable(connectionSource, Form.class, true);
            TableUtils.dropTable(connectionSource, Setup.class, true);
            TableUtils.dropTable(connectionSource, DataType.class, true);

            //resync
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor edit = sharedPrefs.edit();
            String lastUpdate = "2018-01-01 00:00:00";
            edit.putString("form_datatypes_last_update", lastUpdate);
            edit.putString("form_setups_last_update", lastUpdate);
            edit.putString("form_survey_forms_last_update", lastUpdate);
            edit.commit();

            onCreate(sqliteDatabase, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Unable to upgrade database from version " + oldVer + " to new "
                    + newVer, e);
        }
    }

    public Dao<Form, Integer> getFormDao() throws SQLException {
        if (formDao == null) {
            formDao = getDao(Form.class);
        }
        return formDao;
    }

    public Dao<Setup, String> getSetupDao() throws SQLException {
        if (setupDao == null) {
            setupDao = getDao(Setup.class);
        }
        return setupDao;
    }

    public Dao<DataType, String> getDataTypeDao() throws SQLException {
        if (dataTypeDao == null) {
            dataTypeDao = getDao(DataType.class);
        }
        return dataTypeDao;
    }

    public Dao<FormInstance, Integer> getFormInstanceDao() throws SQLException {
        if (formInstanceDao == null) {
            formInstanceDao = getDao(FormInstance.class);
        }
        return formInstanceDao;
    }
}
