package io.legado.app.ui.book.remote

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.remote.manager.RemoteBookWebDav
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.*

class RemoteBookViewModel(application: Application): BaseViewModel(application){
    private val remoteBookFolderName = "book_remote"
    private var dataCallback : DataCallback? = null
    var isRemoteBookLiveData = MutableLiveData<Boolean>()
    var dataFlowStart: (() -> Unit)? = null


    val dataFlow = callbackFlow<List<RemoteBook>> {

        val list = Collections.synchronizedList(ArrayList<RemoteBook>())

        dataCallback = object : DataCallback {

            override fun setItems(remoteFiles: List<RemoteBook>) {
                list.clear()
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun addItems(remoteFiles: List<RemoteBook>) {
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun clear() {
                list.clear()
                trySend(emptyList())
            }
        }
//        withContext(Dispatchers.Main) {
//            dataFlowStart?.invoke()
//        }

        awaitClose {
            dataCallback = null
        }
    }.flowOn(Dispatchers.IO)

    fun loadRemoteBookList() {
        execute {
            dataCallback?.clear()
            val bookList = RemoteBookWebDav.getRemoteBookList()
            dataCallback?.setItems(bookList)
        }
    }



    fun addToBookshelf(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach {
                LocalBook.importFile(Uri.parse(it))
            }
        }.onFinally {
            finally.invoke()
        }
    }

    /**
     * 添加书籍到本地书架
     */
    fun addToBookshelf(remoteBook: RemoteBook, finally: () -> Unit) {
        execute {
            val downloadBookPath = RemoteBookWebDav.getRemoteBook(remoteBook)
            downloadBookPath?.let {
                LocalBook.importFile(Uri.parse(it))
            }
        }.onFinally {
            finally.invoke()
        }
    }
    interface DataCallback {

        fun setItems(remoteFiles: List<RemoteBook>)

        fun addItems(remoteFiles: List<RemoteBook>)

        fun clear()

    }
}

data class RemoteBook(
    val filename: String,
    val urlName: String,
    val size: Long,
    val contentType: String,
    val lastModify: Long
)
