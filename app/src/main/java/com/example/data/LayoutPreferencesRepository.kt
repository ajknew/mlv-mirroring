package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CameraScaleMode
import com.example.model.LayerConfig
import com.example.model.LayerType
import com.example.model.LayoutPreset
import com.example.model.MirrorScaleMode
import com.example.model.MirroringLayoutState
import com.example.model.NormalizedRect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "mlv_layout_preferences")

class LayoutPreferencesRepository(private val context: Context) {

    private object Keys {
        val PRESET = stringPreferencesKey("layout_preset")
        
        val MIRROR_X = floatPreferencesKey("mirror_x")
        val MIRROR_Y = floatPreferencesKey("mirror_y")
        val MIRROR_W = floatPreferencesKey("mirror_w")
        val MIRROR_H = floatPreferencesKey("mirror_h")
        val MIRROR_VISIBLE = booleanPreferencesKey("mirror_visible")
        val MIRROR_Z_INDEX = intPreferencesKey("mirror_z_index")
        val MIRROR_SCALE_MODE = stringPreferencesKey("mirror_scale_mode")
        
        val CAMERA_X = floatPreferencesKey("camera_x")
        val CAMERA_Y = floatPreferencesKey("camera_y")
        val CAMERA_W = floatPreferencesKey("camera_w")
        val CAMERA_H = floatPreferencesKey("camera_h")
        val CAMERA_VISIBLE = booleanPreferencesKey("camera_visible")
        val CAMERA_Z_INDEX = intPreferencesKey("camera_z_index")
        val CAMERA_SCALE_MODE = stringPreferencesKey("camera_scale_mode")
        
        val CAMERA_FACING = stringPreferencesKey("camera_facing")
    }

    val layoutStateFlow: Flow<MirroringLayoutState> = context.layoutDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val presetName = prefs[Keys.PRESET] ?: LayoutPreset.FULLSCREEN_PIP.name
            val preset = try {
                LayoutPreset.valueOf(presetName)
            } catch (e: Exception) {
                LayoutPreset.FULLSCREEN_PIP
            }

            val mirrorX = prefs[Keys.MIRROR_X] ?: 0f
            val mirrorY = prefs[Keys.MIRROR_Y] ?: 0f
            val mirrorW = prefs[Keys.MIRROR_W] ?: 1f
            val mirrorH = prefs[Keys.MIRROR_H] ?: 1f
            val mirrorVis = prefs[Keys.MIRROR_VISIBLE] ?: true
            val mirrorZ = prefs[Keys.MIRROR_Z_INDEX] ?: 0
            val mirrorScale = try {
                MirrorScaleMode.valueOf(prefs[Keys.MIRROR_SCALE_MODE] ?: MirrorScaleMode.FIT.name)
            } catch (e: Exception) {
                MirrorScaleMode.FIT
            }

            val cameraX = prefs[Keys.CAMERA_X] ?: 0.68f
            val cameraY = prefs[Keys.CAMERA_Y] ?: 0.68f
            val cameraW = prefs[Keys.CAMERA_W] ?: 0.28f
            val cameraH = prefs[Keys.CAMERA_H] ?: 0.28f
            val cameraVis = prefs[Keys.CAMERA_VISIBLE] ?: true
            val cameraZ = prefs[Keys.CAMERA_Z_INDEX] ?: 1
            val cameraScale = try {
                CameraScaleMode.valueOf(prefs[Keys.CAMERA_SCALE_MODE] ?: CameraScaleMode.PROPORTIONAL.name)
            } catch (e: Exception) {
                CameraScaleMode.PROPORTIONAL
            }

            MirroringLayoutState(
                mirrorLayer = LayerConfig(
                    type = LayerType.MIRROR,
                    bounds = NormalizedRect(mirrorX, mirrorY, mirrorW, mirrorH).clamped(),
                    isVisible = mirrorVis,
                    zIndex = mirrorZ,
                    mirrorScaleMode = mirrorScale
                ),
                cameraLayer = LayerConfig(
                    type = LayerType.CAMERA,
                    bounds = NormalizedRect(cameraX, cameraY, cameraW, cameraH).clamped(),
                    isVisible = cameraVis,
                    zIndex = cameraZ,
                    cameraScaleMode = cameraScale
                ),
                activePreset = preset
            )
        }

    suspend fun saveLayoutState(state: MirroringLayoutState) {
        context.layoutDataStore.edit { prefs ->
            prefs[Keys.PRESET] = state.activePreset.name
            
            prefs[Keys.MIRROR_X] = state.mirrorLayer.bounds.x
            prefs[Keys.MIRROR_Y] = state.mirrorLayer.bounds.y
            prefs[Keys.MIRROR_W] = state.mirrorLayer.bounds.width
            prefs[Keys.MIRROR_H] = state.mirrorLayer.bounds.height
            prefs[Keys.MIRROR_VISIBLE] = state.mirrorLayer.isVisible
            prefs[Keys.MIRROR_Z_INDEX] = state.mirrorLayer.zIndex
            prefs[Keys.MIRROR_SCALE_MODE] = state.mirrorLayer.mirrorScaleMode.name
            
            prefs[Keys.CAMERA_X] = state.cameraLayer.bounds.x
            prefs[Keys.CAMERA_Y] = state.cameraLayer.bounds.y
            prefs[Keys.CAMERA_W] = state.cameraLayer.bounds.width
            prefs[Keys.CAMERA_H] = state.cameraLayer.bounds.height
            prefs[Keys.CAMERA_VISIBLE] = state.cameraLayer.isVisible
            prefs[Keys.CAMERA_Z_INDEX] = state.cameraLayer.zIndex
            prefs[Keys.CAMERA_SCALE_MODE] = state.cameraLayer.cameraScaleMode.name
        }
    }

    suspend fun resetLayout() {
        val defaultState = MirroringLayoutState.forPreset(LayoutPreset.FULLSCREEN_PIP)
        saveLayoutState(defaultState)
    }
}
