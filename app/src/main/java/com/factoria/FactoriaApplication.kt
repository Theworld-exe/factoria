package com.factoria

import android.app.Application
import com.factoria.core.database.FactoriaDatabase
import com.factoria.core.database.FactoriaRepository
import com.factoria.core.solver.ProductionCalculator

class FactoriaApplication : Application() {

    lateinit var database: FactoriaDatabase
        private set

    lateinit var repository: FactoriaRepository
        private set

    lateinit var calculator: ProductionCalculator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = FactoriaDatabase.getInstance(this)
        repository = FactoriaRepository(database)
        calculator = ProductionCalculator()
    }

    companion object {
        lateinit var instance: FactoriaApplication
            private set
    }
}
