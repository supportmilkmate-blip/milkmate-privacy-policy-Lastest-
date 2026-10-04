package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CattleDao {
    @Query("SELECT * FROM cattle_records WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY tagNumber ASC")
    fun getCattleFlow(businessId: String): Flow<List<CattleEntity>>

    @Query("SELECT * FROM cattle_records WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY tagNumber ASC")
    suspend fun getCattleList(businessId: String): List<CattleEntity>

    @Query("SELECT * FROM cattle_records WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getCattleById(id: String): CattleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCattle(cattle: CattleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCattleList(cattleList: List<CattleEntity>)

    @Update
    suspend fun updateCattle(cattle: CattleEntity)

    @Query("UPDATE cattle_records SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteCattle(id: String, deletedAt: Long = System.currentTimeMillis())

    // --- Cattle Weight Records ---
    @Query("SELECT * FROM cattle_weight_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllWeightsFlow(businessId: String): Flow<List<CattleWeightEntity>>

    @Query("SELECT * FROM cattle_weight_records WHERE cattleId = :cattleId ORDER BY date DESC")
    fun getWeightsForCattleFlow(cattleId: String): Flow<List<CattleWeightEntity>>

    @Query("SELECT * FROM cattle_weight_records WHERE cattleId = :cattleId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeightForCattle(cattleId: String): CattleWeightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: CattleWeightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightList(weightList: List<CattleWeightEntity>)

    @Query("DELETE FROM cattle_weight_records WHERE id = :id")
    suspend fun deleteWeight(id: String)

    // --- California Mastitis Test (CMT) Records ---
    @Query("SELECT * FROM cattle_cmt_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllCmtFlow(businessId: String): Flow<List<CattleCmtEntity>>

    @Query("SELECT * FROM cattle_cmt_records WHERE cattleId = :cattleId ORDER BY date DESC")
    fun getCmtForCattleFlow(cattleId: String): Flow<List<CattleCmtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCmt(cmt: CattleCmtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCmtList(cmtList: List<CattleCmtEntity>)

    @Query("DELETE FROM cattle_cmt_records WHERE id = :id")
    suspend fun deleteCmt(id: String)

    // --- Body Condition Scoring (BCS) Records ---
    @Query("SELECT * FROM cattle_bcs_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllBcsFlow(businessId: String): Flow<List<CattleBcsEntity>>

    @Query("SELECT * FROM cattle_bcs_records WHERE cattleId = :cattleId ORDER BY date DESC")
    fun getBcsForCattleFlow(cattleId: String): Flow<List<CattleBcsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBcs(bcs: CattleBcsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBcsList(bcsList: List<CattleBcsEntity>)

    @Query("DELETE FROM cattle_bcs_records WHERE id = :id")
    suspend fun deleteBcs(id: String)

    // --- Cattle Breeding & Reproduction Records ---
    @Query("SELECT * FROM cattle_breeding_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllBreedingFlow(businessId: String): Flow<List<BreedingRecordEntity>>

    @Query("SELECT * FROM cattle_breeding_records WHERE cattleId = :cattleId ORDER BY date DESC")
    fun getBreedingForCattleFlow(cattleId: String): Flow<List<BreedingRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreeding(record: BreedingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreedingList(records: List<BreedingRecordEntity>)

    @Query("DELETE FROM cattle_breeding_records WHERE id = :id")
    suspend fun deleteBreeding(id: String)

    // --- Cattle Deworming Records ---
    @Query("SELECT * FROM cattle_deworming_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllDewormingFlow(businessId: String): Flow<List<DewormingRecordEntity>>

    @Query("SELECT * FROM cattle_deworming_records WHERE cattleId = :cattleId ORDER BY date DESC")
    fun getDewormingForCattleFlow(cattleId: String): Flow<List<DewormingRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeworming(record: DewormingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDewormingList(records: List<DewormingRecordEntity>)

    @Query("DELETE FROM cattle_deworming_records WHERE id = :id")
    suspend fun deleteDeworming(id: String)

    // --- Cattle Vaccination Records ---
    @Query("SELECT * FROM cattle_vaccination_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllVaccinationFlow(businessId: String): Flow<List<VaccinationRecordEntity>>

    @Query("SELECT * FROM cattle_vaccination_records WHERE cattleId = :cattleId ORDER BY date DESC")
    fun getVaccinationForCattleFlow(cattleId: String): Flow<List<VaccinationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccination(record: VaccinationRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccinationList(records: List<VaccinationRecordEntity>)

    @Query("DELETE FROM cattle_vaccination_records WHERE id = :id")
    suspend fun deleteVaccination(id: String)

    // --- Cattle Treatment Records ---
    @Query("SELECT * FROM cattle_treatment_records WHERE businessId = :businessId ORDER BY checkupDate DESC")
    fun getAllTreatmentFlow(businessId: String): Flow<List<TreatmentRecordEntity>>

    @Query("SELECT * FROM cattle_treatment_records WHERE cattleId = :cattleId ORDER BY checkupDate DESC")
    fun getTreatmentForCattleFlow(cattleId: String): Flow<List<TreatmentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatment(record: TreatmentRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatmentList(records: List<TreatmentRecordEntity>)

    @Query("DELETE FROM cattle_treatment_records WHERE id = :id")
    suspend fun deleteTreatment(id: String)

    // --- Farm Observation Records ---
    @Query("SELECT * FROM farm_observation_records WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllObservationsFlow(businessId: String): Flow<List<FarmObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(record: FarmObservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservationList(records: List<FarmObservationEntity>)

    @Query("DELETE FROM farm_observation_records WHERE id = :id")
    suspend fun deleteObservation(id: String)

    // --- Cattle Milking Records ---
    @Query("SELECT * FROM cattle_milking_records WHERE businessId = :businessId ORDER BY dateEpochMidnight DESC, shift ASC")
    fun getAllMilkingFlow(businessId: String): Flow<List<CattleMilkingRecordEntity>>

    @Query("SELECT * FROM cattle_milking_records WHERE cattleId = :cattleId ORDER BY dateEpochMidnight DESC")
    fun getMilkingForCattleFlow(cattleId: String): Flow<List<CattleMilkingRecordEntity>>

    @Query("SELECT * FROM cattle_milking_records WHERE businessId = :businessId AND dateEpochMidnight = :dateEpochMidnight AND shift = :shift")
    suspend fun getMilkingForShift(businessId: String, dateEpochMidnight: Long, shift: String): List<CattleMilkingRecordEntity>

    @Query("SELECT * FROM cattle_milking_records WHERE businessId = :businessId AND dateEpochMidnight >= :startDate AND dateEpochMidnight <= :endDate")
    suspend fun getMilkingBetweenDates(businessId: String, startDate: Long, endDate: Long): List<CattleMilkingRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilking(record: CattleMilkingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilkingList(records: List<CattleMilkingRecordEntity>)

    @Query("DELETE FROM cattle_milking_records WHERE id = :id")
    suspend fun deleteMilking(id: String)
}
