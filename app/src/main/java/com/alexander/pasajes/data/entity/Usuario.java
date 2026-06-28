package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey
    public int id;
    public String username;
    public String password;
    public int rol;
    public String nombres;
    public String token;
}