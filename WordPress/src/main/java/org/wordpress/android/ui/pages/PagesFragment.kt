package org.wordpress.android.ui.pages

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.pages_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.pages.PageListFragment.Companion.Type
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

class PagesFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PagesViewModel
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private val listStateKey = "list_state"

    companion object {
        fun newInstance(): PagesFragment {
            return PagesFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as WordPress).component()?.inject(this)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<PagesViewModel>(PagesViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pages_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<Toolbar>(org.wordpress.android.login.R.id.toolbar)
        (activity as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setHomeButtonEnabled(true)
        }

        pages_pager.adapter = activity?.let { fragmentActivity ->
            fragmentActivity.supportFragmentManager?.let { manager ->
                PagesPagerAdapter(fragmentActivity, manager)
            }
        }
        tabLayout.setupWithViewPager(pages_pager)

        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(RecyclerItemDecoration(0, DisplayUtils.dpToPx(activity, 1)))

        val adapter = PagesAdapter()
        recyclerView.adapter = adapter
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get<PagesViewModel>(PagesViewModel::class.java)
        viewModel.searchExpanded.observe(activity!!, Observer {
            if (it == true) {
                pages_pager.visibility = View.GONE
                tabLayout.visibility = View.GONE
                pages_search_result.visibility = View.VISIBLE
            } else {
                pages_pager.visibility = View.VISIBLE
                tabLayout.visibility = View.VISIBLE
                pages_search_result.visibility = View.GONE
            }
        })
        viewModel.searchResult.observe(this, Observer { result ->
            if (result != null) {
                recyclerView.visibility = View.VISIBLE
                adapter.onNext(result)
            } else {
                recyclerView.visibility = View.GONE
                adapter.onNext(listOf())
            }
        })

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(pullToRefresh) { viewModel.refresh() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_search, menu)
        val myActionMenuItem = checkNotNull(menu.findItem(R.id.action_search)) {
            "Menu does not contain mandatory search item"
        }
        viewModel.searchExpanded.observe(activity!!, Observer {
            if (it == true) {
                pages_pager.visibility = View.GONE
                tabLayout.visibility = View.GONE
                pages_search_result.visibility = View.VISIBLE
                if (!myActionMenuItem.isActionViewExpanded) {
                    myActionMenuItem.expandActionView()
                }
            } else {
                pages_pager.visibility = View.VISIBLE
                tabLayout.visibility = View.VISIBLE
                pages_search_result.visibility = View.GONE
                if (myActionMenuItem.isActionViewExpanded) {
                    myActionMenuItem.collapseActionView()
                }
            }
        })

        myActionMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return viewModel.searchExpanded()
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                return viewModel.searchCollapsed()
            }
        })
        val searchView = myActionMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return viewModel.onSearchTextSubmit(query)
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return viewModel.onSearchTextChange(newText)
            }
        })
    }
}

class PagesPagerAdapter(val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int = 4

    override fun getItem(position: Int): Fragment {
        return PageListFragment.newInstance("key$position", Type.getType(position))
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return Type.getType(position).text.let { context.getString(it) }
    }
}
