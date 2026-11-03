package com.mika.domain.data.model.home

import com.mika.domain.data.model.home.chart.Chart
import com.mika.domain.data.model.mood.Mood
import com.mika.domain.utils.Resource

data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)