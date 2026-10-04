package dev.asrithtanniru.dotwall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.asrithtanniru.dotwall.model.ApplyTarget
import dev.asrithtanniru.dotwall.model.BackgroundStyle
import dev.asrithtanniru.dotwall.model.Density
import dev.asrithtanniru.dotwall.model.DotShape
import dev.asrithtanniru.dotwall.model.Goal
import dev.asrithtanniru.dotwall.model.Look
import dev.asrithtanniru.dotwall.model.Mode
import dev.asrithtanniru.dotwall.model.Placement
import dev.asrithtanniru.dotwall.model.RenderSpec
import dev.asrithtanniru.dotwall.model.WeekStart
import dev.asrithtanniru.dotwall.model.YearStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dots")

/** Last successful apply, used to skip redundant work. */
data class AppliedState(val date: LocalDate?, val hash: Int?, val epochMillis: Long?)

class SettingsRepo(private val context: Context) {
    private val store get() = context.dataStore

    val spec: Flow<RenderSpec> = store.data.map { it.toSpec() }
    val applied: Flow<AppliedState> = store.data.map { it.toApplied() }

    suspend fun currentSpec(): RenderSpec = spec.first()
    suspend fun currentApplied(): AppliedState = applied.first()

    suspend fun update(transform: (RenderSpec) -> RenderSpec) {
        store.edit { prefs -> prefs.writeSpec(transform(prefs.toSpec())) }
    }

    suspend fun markApplied(date: LocalDate, hash: Int, epochMillis: Long) {
        store.edit {
            it[K_APPLIED_DATE] = date.toString()
            it[K_APPLIED_HASH] = hash
            it[K_APPLIED_AT] = epochMillis.toString()
        }
    }

    private fun Preferences.toApplied() = AppliedState(
        date = this[K_APPLIED_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        hash = this[K_APPLIED_HASH],
        epochMillis = this[K_APPLIED_AT]?.toLongOrNull(),
    )

    private fun Preferences.toSpec(): RenderSpec {
        val d = RenderSpec()
        return RenderSpec(
            mode = enumOf(this[K_MODE], d.mode),
            yearStyle = enumOf(this[K_YEAR_STYLE], d.yearStyle),
            weekStart = enumOf(this[K_WEEK_START], d.weekStart),
            birthDate = this[K_BIRTH]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: d.birthDate,
            lifespanYears = (this[K_LIFESPAN] ?: d.lifespanYears).coerceIn(50, 100),
            goals = decodeGoals(this[K_GOALS]),
            activeGoalId = this[K_ACTIVE_GOAL],
            look = Look(
                accent = this[K_ACCENT] ?: d.look.accent,
                background = enumOf(this[K_BG], d.look.background),
                shape = enumOf(this[K_SHAPE], d.look.shape),
                density = enumOf(this[K_DENSITY], d.look.density),
                showFooter = this[K_FOOTER] ?: d.look.showFooter,
                showGoalTitle = this[K_TITLE] ?: d.look.showGoalTitle,
            ),
            placement = Placement(
                top = this[K_TOP] ?: d.placement.top,
                bottom = this[K_BOTTOM] ?: d.placement.bottom,
                side = this[K_SIDE] ?: d.placement.side,
            ),
            target = enumOf(this[K_TARGET], d.target),
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeSpec(s: RenderSpec) {
        this[K_MODE] = s.mode.name
        this[K_YEAR_STYLE] = s.yearStyle.name
        this[K_WEEK_START] = s.weekStart.name
        this[K_BIRTH] = s.birthDate.toString()
        this[K_LIFESPAN] = s.lifespanYears
        this[K_GOALS] = encodeGoals(s.goals)
        if (s.activeGoalId != null) this.set(K_ACTIVE_GOAL, s.activeGoalId) else this.remove(K_ACTIVE_GOAL)
        this[K_ACCENT] = s.look.accent
        this[K_BG] = s.look.background.name
        this[K_SHAPE] = s.look.shape.name
        this[K_DENSITY] = s.look.density.name
        this[K_FOOTER] = s.look.showFooter
        this[K_TITLE] = s.look.showGoalTitle
        this[K_TOP] = s.placement.top
        this[K_BOTTOM] = s.placement.bottom
        this[K_SIDE] = s.placement.side
        this[K_TARGET] = s.target.name
    }

    private companion object {
        val K_MODE = stringPreferencesKey("mode")
        val K_YEAR_STYLE = stringPreferencesKey("year_style")
        val K_WEEK_START = stringPreferencesKey("week_start")
        val K_BIRTH = stringPreferencesKey("birth")
        val K_LIFESPAN = intPreferencesKey("lifespan")
        val K_GOALS = stringPreferencesKey("goals")
        val K_ACTIVE_GOAL = stringPreferencesKey("active_goal")
        val K_ACCENT = intPreferencesKey("accent")
        val K_BG = stringPreferencesKey("background")
        val K_SHAPE = stringPreferencesKey("shape")
        val K_DENSITY = stringPreferencesKey("density")
        val K_FOOTER = booleanPreferencesKey("footer")
        val K_TITLE = booleanPreferencesKey("goal_title")
        val K_TOP = floatPreferencesKey("top")
        val K_BOTTOM = floatPreferencesKey("bottom")
        val K_SIDE = floatPreferencesKey("side")
        val K_TARGET = stringPreferencesKey("target")
        val K_APPLIED_DATE = stringPreferencesKey("applied_date")
        val K_APPLIED_HASH = intPreferencesKey("applied_hash")
        val K_APPLIED_AT = stringPreferencesKey("applied_at")

        inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default

        fun encodeGoals(goals: List<Goal>): String = JSONArray().apply {
            goals.forEach {
                put(JSONObject().put("id", it.id).put("title", it.title)
                    .put("start", it.start.toString()).put("end", it.end.toString())
                    .put("missed", it.missed))
            }
        }.toString()

        fun decodeGoals(json: String?): List<Goal> {
            if (json.isNullOrEmpty()) return emptyList()
            return runCatching {
                val arr = JSONArray(json)
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Goal(o.getString("id"), o.getString("title"),
                        LocalDate.parse(o.getString("start")), LocalDate.parse(o.getString("end")),
                        o.optBoolean("missed", false))
                }
            }.getOrDefault(emptyList())
        }
    }
}
