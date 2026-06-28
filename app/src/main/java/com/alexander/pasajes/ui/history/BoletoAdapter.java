package com.alexander.pasajes.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alexander.pasajes.data.entity.Boleto;
import java.util.List;
import java.util.Locale; // ✅ Añadido para estandarizar el formateo de precios en Soles

public class BoletoAdapter extends RecyclerView.Adapter<BoletoAdapter.ViewHolder> {

    private final List<Boleto> boletos;
    private OnItemLongClickListener longClickListener;

    // Interfaz para delegar el evento táctil analítico de anulación al Fragment
    public interface OnItemLongClickListener {
        void onItemLongClick(Boleto b);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public BoletoAdapter(List<Boleto> boletos) {
        this.boletos = boletos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño nativo de dos líneas de subtítulo
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Boleto b = boletos.get(position);

        // Formateo limpio del pasaje (Línea principal: Categoría y Costo)
        holder.text1.setText(String.format(Locale.getDefault(), "%s - S/ %.2f", b.tipoPasajero, (b.precioCentavos / 100.0)));

        // Formateo analítico perimetral (Subtítulo: Pasarela de pago, Ruta y Estado de Auditoría)
        String estado = b.anulado ? "ANULADO ❌" : "Válido ✓";
        holder.text2.setText(String.format(Locale.getDefault(), "%s [%s → %s] - %s", b.metodoPago, b.origen, b.destino, estado));

        // 🚀 SOLUCIÓN EXCLUSIVA: Activar físicamente el gatillo del click largo sobre la tarjeta
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(b);
            }
            return true; // Indica a Android que consumió el evento de presión larga de manera exitosa
        });
    }

    @Override
    public int getItemCount() {
        return boletos != null ? boletos.size() : 0;
    }

    // Contenedor estático para reciclaje eficiente de memoria en el scrolling de la lista
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        ViewHolder(View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }
}