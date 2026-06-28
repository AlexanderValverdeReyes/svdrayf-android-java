package com.alexander.pasajes.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.alexander.pasajes.data.entity.Turno;

@Dao
public interface TurnoDao {
    @Insert
    long insert(Turno turno);

    @Update
    void update(Turno turno);

    @Query("SELECT * FROM turnos WHERE activo = 1 LIMIT 1")
    Turno getTurnoActivo();

    @Query("SELECT * FROM turnos WHERE vendedorId = :vendedorId AND activo = 1 LIMIT 1")
    Turno getTurnoActivoPorVendedor(int vendedorId);

    // 🚀 SOLUCIÓN AL ERROR: Declaramos la consulta exacta que requiere Room para compilar
    @Query("SELECT * FROM turnos WHERE id = :turnoId LIMIT 1")
    Turno getTurnoPorId(int turnoId);
}