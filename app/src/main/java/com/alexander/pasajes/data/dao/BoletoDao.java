package com.alexander.pasajes.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.alexander.pasajes.data.entity.Boleto;
import java.util.List;

@Dao
public interface BoletoDao {
    @Insert
    void insert(Boleto boleto);

    @Update
    void update(Boleto boleto);

    @Query("SELECT * FROM boletos WHERE turnoId = :turnoId ORDER BY fechaHora DESC")
    List<Boleto> getBoletosPorTurno(int turnoId);

    @Query("UPDATE boletos SET anulado = 1 WHERE id = :boletoId")
    void anularBoleto(int boletoId);

    @Query("SELECT * FROM boletos WHERE sincronizado = 0")
    List<Boleto> getBoletosNoSincronizados();

    @Query("UPDATE boletos SET sincronizado = 1 WHERE id = :boletoId")
    void marcarSincronizado(int boletoId);

    //  SOLUCIÓN RFN50: Elimina de la memoria local los boletos ya subidos a Neon DB
    @Query("DELETE FROM boletos WHERE turnoId = :turnoId AND sincronizado = 1")
    void clearBoletosSincronizadosTurno(int turnoId);
}