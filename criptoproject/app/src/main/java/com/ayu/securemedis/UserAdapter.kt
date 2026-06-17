package com.ayu.securemedis

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ayu.securemedis.databinding.ItemUserBinding

/**
 * Adapter untuk menampilkan daftar semua user di AdminActivity.
 * Mendukung aksi hapus user via callback.
 */
class UserAdapter(
    private val onDelete: (User) -> Unit
) : ListAdapter<User, UserAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvUsername.text = user.username
            binding.tvRole.text = user.role
            binding.ivAvatar.text = user.username.first().uppercase()

            // Warna role badge
            val roleColor = when (user.role) {
                "DOKTER" -> binding.root.context.getColor(R.color.role_dokter)
                "PASIEN" -> binding.root.context.getColor(R.color.role_pasien)
                "ADMIN"  -> binding.root.context.getColor(R.color.role_admin)
                else     -> binding.root.context.getColor(R.color.primary)
            }
            binding.tvRole.setTextColor(roleColor)
            binding.cardRole.strokeColor = roleColor

            binding.btnDelete.setOnClickListener { onDelete(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
