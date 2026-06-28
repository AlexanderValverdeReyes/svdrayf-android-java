package com.alexander.pasajes.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.alexander.pasajes.data.dao.*;
import com.alexander.pasajes.data.entity.*;

@Database(entities = {
        Usuario.class, Bus.class, Ruta.class, Paradero.class, Tarifa.class,
        Turno.class, Boleto.class, MotivoAnulacion.class,
        ConfiguracionEmpresa.class   // ← Entidad correcta, no el DTO
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UsuarioDao usuarioDao();
    public abstract MaestrosDao maestrosDao();
    public abstract TurnoDao turnoDao();
    public abstract BoletoDao boletoDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pasajes-db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}