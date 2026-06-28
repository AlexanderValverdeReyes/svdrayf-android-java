package com.alexander.pasajes.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.alexander.pasajes.data.entity.Usuario;

@Dao
public interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE username = :user AND password = :pass LIMIT 1")
    Usuario loginLocal(String user, String pass);
}