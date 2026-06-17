package com.ayu.securemedis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ayu.securemedis.databinding.ItemRekamMedisBinding

/**
 * Adapter untuk menampilkan daftar rekam medis yang sudah didekripsi.
 * Digunakan di PasienActivity dan RekamMedisListActivity.
 */
class RekamMedisAdapter(
    private val onItemClick: ((RekamMedisDecrypted) -> Unit)? = null
) : ListAdapter<RekamMedisDecrypted, RekamMedisAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemRekamMedisBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RekamMedisDecrypted) {
            binding.tvTanggal.text = "📅 ${item.tanggal}"
            binding.tvDiagnosis.text = "🩺 Diagnosis: ${item.diagnosis}"
            binding.tvResep.text = "💊 Resep: ${item.resep}"
            binding.tvCatatan.text = "📝 Catatan: ${item.catatan}"

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRekamMedisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RekamMedisDecrypted>() {
        override fun areItemsTheSame(oldItem: RekamMedisDecrypted, newItem: RekamMedisDecrypted) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RekamMedisDecrypted, newItem: RekamMedisDecrypted) =
            oldItem == newItem
    }
}
