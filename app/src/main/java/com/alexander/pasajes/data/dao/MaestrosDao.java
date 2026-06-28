package com.alexander.pasajes.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.alexander.pasajes.data.entity.*;
import java.util.List;

@Dao
public interface MaestrosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBuses(List<Bus> buses);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRutas(List<Ruta> rutas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertParaderos(List<Paradero> paraderos);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTarifas(List<Tarifa> tarifas);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMotivosAnulacion(List<MotivoAnulacion> motivos);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConfiguracion(ConfiguracionEmpresa config);

    @Query("SELECT * FROM configuracion_empresa LIMIT 1")
    ConfiguracionEmpresa getConfiguracion();

    @Query("SELECT * FROM buses")
    List<Bus> getAllBuses();

    @Query("SELECT * FROM rutas")
    List<Ruta> getAllRutas();

    @Query("SELECT * FROM motivos_anulacion")
    List<MotivoAnulacion> getMotivosAnulacion();

    //  CORREGIDO: Ahora apunta al nombre físico 'configuracion_empresa' definido en la entidad
    @Query("SELECT * FROM buses WHERE id = :busId LIMIT 1")
    Bus getBusById(int busId);

    @Query("SELECT * FROM tarifas WHERE rutaId = :rutaId AND origenParaderoId = :origen AND destinoParaderoId = :destino AND tipoPasajero = :tipo")
    Tarifa getTarifa(int rutaId, int origen, int destino, String tipo);

    @Query("SELECT * FROM tarifas WHERE rutaId = :rutaId AND tipoPasajero = :tipo")
    List<Tarifa> getTarifasPorRutaYTipo(int rutaId, String tipo);

    @Query("SELECT DISTINCT origenParaderoId, destinoParaderoId FROM tarifas WHERE rutaId = :rutaId")
    List<TramoIds> getTramosDistintos(int rutaId);

    @Query("SELECT * FROM paradero WHERE id_paradero = :id")
    Paradero getParaderoById(int id);

    class TramoIds {
        public int origenParaderoId;
        public int destinoParaderoId;
    }

    @Query("SELECT DISTINCT tipoPasajero FROM tarifas WHERE rutaId = :rutaId AND origenParaderoId = :origen AND destinoParaderoId = :destino")
    List<String> getTiposPasajeroPorTramo(int rutaId, int origen, int destino);
}