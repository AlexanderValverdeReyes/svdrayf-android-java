package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "boletos")
public class Boleto {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int turnoId;                 // ID del turno al que pertenece
    public String tipoPasajero;         // "General", "Universitario", "Frecuente"
    public String uuid;
    public int precioCentavos;          // Precio en céntimos de Sol (S/.)
    public String metodoPago;           // "EFECTIVO" o "QR"
    public long fechaHora;              // Timestamp de emisión
    public boolean anulado;             // false = válido, true = anulado
    public boolean sincronizado;
    public int tarifarioId = 0;// false = pendiente de subir al servidor

    // Nuevos campos para el hash y datos de ruta
    public String hash;                 // Token criptográfico (payload + firma)
    public String origen;               // Nombre del paradero origen (si aplica)
    public String destino;              // Nombre del paradero destino (si aplica)
    public String rutaNombre;           // Nombre de la ruta o modalidad
    public boolean huboIntentoQR;      // true si se mostró QR aunque se pagó en efectivo
    public String estadoInspector;     // null, "VALIDADO", "RECHAZADO"
}