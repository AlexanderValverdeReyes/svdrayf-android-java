package com.alexander.pasajes.repository;

import android.content.Context;
import com.alexander.pasajes.data.db.AppDatabase;
import com.alexander.pasajes.data.entity.*;
import com.alexander.pasajes.network.model.*;
import java.util.ArrayList;
import java.util.List;
import com.alexander.pasajes.data.dao.MaestrosDao;

public class AppRepository {
    private AppDatabase db;

    public AppRepository(Context context) {
        db = AppDatabase.getInstance(context);   // ← ahora sí existe
    }

    // ===== USUARIOS =====
    public void insertOrUpdateUsuario(Usuario usuario) {
        db.usuarioDao().insertOrUpdate(usuario);
    }

    public Usuario loginLocal(String user, String pass) {
        return db.usuarioDao().loginLocal(user, pass);
    }

    // ===== MAESTROS =====
    public void guardarMaestros(MaestrosResponse response) {
        // 1. Buses
        List<Bus> buses = new ArrayList<>();
        for (BusDTO dto : response.getBuses()) {
            Bus b = new Bus();
            b.id = dto.idBus;
            b.placa = dto.placa;
            b.descripcion = dto.placa + " - " + dto.numeroPadron;
            buses.add(b);
        }
        db.maestrosDao().insertBuses(buses);

        // 2. Rutas
        List<Ruta> rutas = new ArrayList<>();
        for (RutaDTO dto : response.getRutas()) {
            Ruta r = new Ruta();
            r.id = dto.idRutaModalidad;
            r.nombre = dto.nombreModalidad;
            if (dto.nombreModalidad.toUpperCase().contains("MALA") || dto.nombreModalidad.toUpperCase().contains("PARADERO")) {
                r.tipo = "PARADEROS";
            } else {
                r.tipo = "DIRECTO";
            }
            rutas.add(r);
        }
        db.maestrosDao().insertRutas(rutas);

        // 3. Paraderos
        List<Paradero> paraderos = new ArrayList<>();
        for (ParaderoDTO dto : response.getParaderos()) {
            Paradero p = new Paradero();
            p.id = dto.idParadero;
            p.nombre = dto.nombreParadero;
            paraderos.add(p);
        }
        db.maestrosDao().insertParaderos(paraderos);

        // 4. Tarifas
        List<TipoPasajeroDTO> tiposPasajero = response.getTiposPasajero();
        java.util.Map<Integer, String> mapaTipos = new java.util.HashMap<>();
        if (tiposPasajero != null) {
            for (TipoPasajeroDTO tp : tiposPasajero) {
                mapaTipos.put(tp.idTipoPasajero, tp.nombreTipo);
            }
        }

        List<Tarifa> tarifas = new ArrayList<>();
        for (TarifaDTO dto : response.getTarifas()) {
            Tarifa t = new Tarifa();
            t.id = dto.idTarifario;
            t.rutaId = dto.idRutaModalidad;
            t.origenParaderoId = dto.idParaderoOrigen;
            t.destinoParaderoId = dto.idParaderoDestino;
            String nombreTipo = mapaTipos.get(dto.idTipoPasajero);
            t.tipoPasajero = nombreTipo != null ? nombreTipo : "General";
            t.precioCentavos = dto.precioNormalCentavos;
            t.precioDomFerCentavos = dto.precioDomFerCentavos;
            tarifas.add(t);
        }
        db.maestrosDao().insertTarifas(tarifas);

        // 5. Motivos de anulación
        List<MotivoAnulacion> motivos = new ArrayList<>();
        for (MotivoAnulacionDTO dto : response.getMotivosAnulacion()) {
            MotivoAnulacion m = new MotivoAnulacion();
            m.id = dto.idMotivo;
            m.descripcion = dto.descripcionMotivo;
            motivos.add(m);
        }
        db.maestrosDao().insertMotivosAnulacion(motivos);

        // 6. Configuración de empresa (mapeo correcto de DTO a entidad)
        if (response.getConfiguracion() != null) {
            ConfiguracionEmpresaDTO dtoConfig = response.getConfiguracion();
            ConfiguracionEmpresa config = new ConfiguracionEmpresa();
            config.id = dtoConfig.idConfig;          // campo del DTO: @SerializedName("id_config")
            config.razonSocial = dtoConfig.razonSocial;
            config.ruc = dtoConfig.ruc;
            config.direccionFiscal = dtoConfig.direccionFiscal;
            config.leyendaPie = dtoConfig.leyendaPie;
            db.maestrosDao().insertConfiguracion(config);
        }
    }

    // ===== MÉTODOS DE ACCESO =====
    public List<Bus> getAllBuses() {
        return db.maestrosDao().getAllBuses();
    }

    public List<Ruta> getRutas() {
        return db.maestrosDao().getAllRutas();
    }

    public Bus getBus(int busId) {
        return db.maestrosDao().getBusById(busId);
    }

    public Tarifa getTarifa(int rutaId, int origen, int destino, String tipo) {
        return db.maestrosDao().getTarifa(rutaId, origen, destino, tipo);
    }

    // ===== TURNOS =====
    public long abrirTurno(Turno turno) {
        return db.turnoDao().insert(turno);
    }

    public Turno getTurnoActivo() {
        return db.turnoDao().getTurnoActivo();
    }

    public void cerrarTurno(Turno turno) {
        db.turnoDao().update(turno);
    }

    public Turno getTurnoActivoPorVendedor(int vendedorId) {
        return db.turnoDao().getTurnoActivoPorVendedor(vendedorId);  // ← switchToDao no existe
    }
    // Colocar dentro de AppRepository.java en la sección de TURNOS:
    public Turno getTurnoPorId(int turnoId) {
        return db.turnoDao().getTurnoPorId(turnoId);
    }

    // ===== BOLETOS =====
    public void venderBoleto(Boleto boleto) {
        db.boletoDao().insert(boleto);
    }

    public List<Boleto> getBoletosTurno(int turnoId) {
        return db.boletoDao().getBoletosPorTurno(turnoId);
    }

    public void anularBoleto(int boletoId) {
        db.boletoDao().anularBoleto(boletoId);
    }

    public List<Boleto> getBoletosNoSincronizados() {
        return db.boletoDao().getBoletosNoSincronizados();
    }

    public void marcarSincronizado(int id) {
        db.boletoDao().marcarSincronizado(id);
    }

    // ===== PARADEROS / TRAMOS =====
    public List<String> getTiposPasajeroPorTramo(int rutaId, int origen, int destino) {
        return db.maestrosDao().getTiposPasajeroPorTramo(rutaId, origen, destino);
    }

    public List<MaestrosDao.TramoIds> getTramosDistintos(int rutaId) {
        return db.maestrosDao().getTramosDistintos(rutaId);
    }

    public Paradero getParaderoById(int id) {
        return db.maestrosDao().getParaderoById(id);
    }

    public ConfiguracionEmpresa getConfiguracion() {
        return db.maestrosDao().getConfiguracion();   // ahora devuelve entidad
    }
}