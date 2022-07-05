package com.aa.plexautodelete.plex.model


data class ShowSection(override val key: String, override val title: String, val shows: List<Show>) : Section