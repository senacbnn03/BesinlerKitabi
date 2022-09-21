package com.example.besinlerkitabi.viewmodel
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.besinlerkitabi.model.Besin
import com.example.besinlerkitabi.servis.BesinAPIServis
import com.example.besinlerkitabi.servis.BesinDatabase
import com.example.besinlerkitabi.util.OzelSharedPreferences
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch

class BesinListesiViewModel(application: Application):BaseViewModel(application){
    val besinler=MutableLiveData<List<Besin>>()
    val besinHataMesaji=MutableLiveData<Boolean>()
    val besinYukleniyor=MutableLiveData<Boolean>()

    private var guncellemeZamani=10*60*1000*1000*1000L   //Dakikanın nanotime'a çevrilmiş hali.


    private val besinApiServis=BesinAPIServis()
    private val disposable=CompositeDisposable()
    private val ozelSharedPreferences=OzelSharedPreferences(getApplication())

     fun refreshData(){

         val kaydedilmeZamani=ozelSharedPreferences.zamaniAl()
         if(kaydedilmeZamani!=null && kaydedilmeZamani!=0L && System.nanoTime()-kaydedilmeZamani<guncellemeZamani){
             // İnternetten almaya gerek yok.SQLite'dan veriler alınmalı.
             verileriSQLitetanAl()
         }else{
             verileriInternettenAl()
         }
     }
    fun refreshFromInternet(){
        verileriInternettenAl()
    }

    private fun verileriSQLitetanAl(){
        besinYukleniyor.value=true

        launch {
           val besinListesi= BesinDatabase(getApplication()).besinDao().getAllBesin()
            besinleriGoster(besinListesi)
            Toast.makeText(getApplication(),"Besinleri Room'dan aldık",Toast.LENGTH_LONG).show()
        }
    }

    private fun verileriInternettenAl(){
        besinYukleniyor.value=true
        disposable.add(
                besinApiServis.getData()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object :DisposableSingleObserver<List<Besin>>(){
                            override fun onSuccess(t: List<Besin>) {
                                //Başarılı olunursa
                               // besinler.value=t
                               // besinHataMesaji.value=false
                               // besinYukleniyor.value=false
                                sqliteSakla(t)
                                Toast.makeText(getApplication(),"Besinleri Internet'ten aldık",Toast.LENGTH_LONG).show()
                            }
                            override fun onError(e: Throwable) {
                                //Hata alınırsa
                                besinHataMesaji.value=true
                                besinYukleniyor.value=false
                                e.printStackTrace()
                            }
                        })
        )
    }
    private fun besinleriGoster(besinlerListesi:List<Besin>){
        besinler.value=besinlerListesi
        besinHataMesaji.value=false
        besinYukleniyor.value=false
    }
    private fun sqliteSakla(besinListesi:List<Besin>){
        launch {
            val dao=BesinDatabase(getApplication()).besinDao()
            dao.deleteAllBesin()
            val uuidListesi= dao.insertAll(*besinListesi.toTypedArray())
            var i=0
            while (i<besinListesi.size){
                besinListesi[i].uuid=uuidListesi[i].toInt()
                i=i+1
            }
            besinleriGoster(besinListesi)
        }
        ozelSharedPreferences.zamaniKaydet(System.nanoTime())
    }
}




