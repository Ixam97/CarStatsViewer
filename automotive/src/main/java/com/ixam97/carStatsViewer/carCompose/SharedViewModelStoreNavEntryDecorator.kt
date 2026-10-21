package com.ixam97.carStatsViewer.carCompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

@Composable
fun <T : Any> rememberSharedViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
    removeViewModelStoreOnPop: () -> Boolean = { true },
): SharedViewModelStoreNavEntryDecorator<T> {
    val currentRemoveViewModelStoreOnPop = rememberUpdatedState(removeViewModelStoreOnPop)
    return remember(viewModelStoreOwner, currentRemoveViewModelStoreOnPop) {
        SharedViewModelStoreNavEntryDecorator(
            viewModelStoreOwner.viewModelStore,
            removeViewModelStoreOnPop,
        )
    }
}

class SharedViewModelStoreNavEntryDecorator<T: Any>(
    viewModelStore: ViewModelStore,
    removeViewModelStoreOnPop: () -> Boolean
) : NavEntryDecorator<T>(
    onPop = ({ key ->
        if (removeViewModelStoreOnPop()) {
            viewModelStore.getEntryViewModel().clearViewModelSoreOwnerForKey(key)
        }
    }),
    decorate = { entry ->
        val standaloneViewModelStore =
            viewModelStore.getEntryViewModel().viewModelStoreForKey(entry.contentKey)
        val standaloneViewModelStoreOwner =
            rememberViewModelStoreOwner(standaloneViewModelStore)

        // If the entry indicates it has a parent, use its parent's ViewModelStore.
        val parentViewModelStore = entry.metadata[ParentKey]?.let {
            viewModelStore.getEntryViewModel().viewModelStoreForKey(it)
        }
        val parentViewModelStoreOwner = parentViewModelStore?.let {
            rememberViewModelStoreOwner(it)
        }
        CompositionLocalProvider(LocalViewModelStoreOwner provides standaloneViewModelStoreOwner) {
            if (parentViewModelStoreOwner != null) {
                CompositionLocalProvider(
                    LocalSharedViewModelStoreOwner provides parentViewModelStoreOwner
                ) {
                    entry.Content()
                }
            } else {
                entry.Content()
            }
        }
    },
) {
    companion object {
        fun parent(key: Any) = metadata {
            put(ParentKey, key)
        }
        fun parents(vararg parents: Any) = metadata {
            put(ParentKey, parents.toList())
        }

        object ParentKey: NavMetadataKey<Any>
    }
}

private class EntryViewModel: ViewModel() {
    private val owners = mutableMapOf<Any, ViewModelStore>()

    fun viewModelStoreForKey(key: Any): ViewModelStore {
        if (key is List<*>) {
            key.forEach {
                if (it != null && owners.contains(it)) return owners.getOrPut(it) { ViewModelStore() }
            }
        }
        return owners.getOrPut(key) { ViewModelStore() }
    }

    fun clearViewModelSoreOwnerForKey(key: Any) {
        owners.remove(key)?.clear()
    }

    override fun onCleared() {
        owners.forEach { (_, store) -> store.clear() }
    }
}

private fun ViewModelStore.getEntryViewModel(): EntryViewModel {
    val provider = ViewModelProvider.create(
        store = this,
        factory = viewModelFactory { initializer { EntryViewModel() } }
    )
    return provider[EntryViewModel::class]
}

val LocalSharedViewModelStoreOwner =
    staticCompositionLocalOf<ViewModelStoreOwner> { error("No LocalSharedViewModelStoreOwner provided!") }

@Composable
fun rememberViewModelStoreOwner(viewModelStore: ViewModelStore): ViewModelStoreOwner {
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    return remember(viewModelStore, savedStateRegistryOwner) {
        object :
            ViewModelStoreOwner,
            SavedStateRegistryOwner by savedStateRegistryOwner,
            HasDefaultViewModelProviderFactory {
                override val viewModelStore: ViewModelStore
                    get() = viewModelStore
                override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                    get() = SavedStateViewModelFactory()
                override val defaultViewModelCreationExtras: CreationExtras
                    get() = MutableCreationExtras().also {
                        it[SAVED_STATE_REGISTRY_OWNER_KEY] = this
                        it[VIEW_MODEL_STORE_OWNER_KEY] = this
                    }
                init {
                    require(this.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
                        "The Lifecycle state is already beyond INITIALIZED. The " +
                                "SharedViewModelStoreNavEntryDecorator requires adding the " +
                                "SavedStateNavEntryDecorator to ensure support for " +
                                "SavedStateHandles."
                    }
                    enableSavedStateHandles()
                }
            }
    }
}

fun NavKey.toContentKey() = this.toString()