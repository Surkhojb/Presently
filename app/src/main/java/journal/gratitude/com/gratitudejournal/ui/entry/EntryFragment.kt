package journal.gratitude.com.gratitudejournal.ui.entry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import journal.gratitude.com.gratitudejournal.databinding.EntryFragmentBinding
import journal.gratitude.com.gratitudejournal.model.EDITED_EXISTING_ENTRY
import journal.gratitude.com.gratitudejournal.model.SHARED_ENTRY
import journal.gratitude.com.gratitudejournal.repository.EntryRepository
import journal.gratitude.com.gratitudejournal.room.EntryDatabase
import journal.gratitude.com.gratitudejournal.ui.dialog.CelebrateDialogFragment
import kotlinx.android.synthetic.main.entry_fragment.*
import org.threeten.bp.LocalDate


class EntryFragment : Fragment() {

    private lateinit var viewModel: EntryViewModel
    private lateinit var binding: EntryFragmentBinding
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = EntryFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val passedInDate = arguments?.getString(DATE) ?: LocalDate.now().toString()
        val entryDao = EntryDatabase.getDatabase(activity!!.application).entryDao()

        val repository = EntryRepository(entryDao)

        viewModel = ViewModelProviders.of(
                this,
                EntryViewModelFactory(passedInDate, repository, activity!!.application)
        ).get(EntryViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(context!!)

        viewModel.entry.observe(this, Observer {
            binding.viewModel = viewModel
        })

        share_button.setOnClickListener {
            firebaseAnalytics.logEvent(SHARED_ENTRY, null)

            val message = viewModel.getShareContent()
            val share = Intent(Intent.ACTION_SEND)
            share.type = "text/plain"
            share.putExtra(Intent.EXTRA_TEXT, message)

            startActivity(Intent.createChooser(share, "Share your gratitude"))
        }

        save_button.setOnClickListener {
            val numEntries = arguments?.getInt(NUM_ENTRIES) ?: 0
            val isNewEntry = arguments?.getBoolean(IS_NEW_ENTRY) ?: false
            if (isNewEntry) {
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.LEVEL, (numEntries + 1).toString())
                val milestones = arrayOf(5, 10, 25, 50, 100, 150, 200, 250, 300)
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_UP, bundle)
                if (milestones.contains(numEntries + 1)) {
                    CelebrateDialogFragment.newInstance(numEntries + 1).show(fragmentManager!!, "NoticeDialogFragment")
                }
            } else {
                firebaseAnalytics.logEvent(EDITED_EXISTING_ENTRY, null)
            }

            viewModel.addNewEntry()
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(entry_text.windowToken, 0)
            findNavController().navigateUp()
        }
    }

    companion object {
        const val DATE = "date_key"
        const val IS_NEW_ENTRY = "is_new_entry"
        const val NUM_ENTRIES = "num_entries"

        fun newInstance(date: LocalDate = LocalDate.now()): EntryFragment {
            val fragment = EntryFragment()

            val bundle = Bundle()
            bundle.putString(DATE, date.toString())
            fragment.arguments = bundle

            return fragment
        }

    }
}
