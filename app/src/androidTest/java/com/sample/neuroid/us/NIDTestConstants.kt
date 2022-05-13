package com.sample.neuroid.us

const val NID_STRUCT_CREATE_SESSION = "\\{\"type\":\"CREATE_SESSION\",\"ts\":\\d{13,},\"f\":\"(.*?)\",\"sid\":\"(.*?)\",\"cid\":\"(.*?)\",\"did\":\"(.*?)\",\"iid\":\"(.*?)\",\"loc\":\"(.*?)\",\"ua\":\"(.*?)\",\"tzo\":(.*?),\"lng\":\"(.*?)\",\"ce\":true,\"je\":true,\"ol\":true,\"p\":\"Android\",\"dnt\":false,\"url\":\"(.*?)\",\"ns\":\"nid\",\"jsl\":\\[\\],\"jsv\":\"(.*?)\"\\}"
const val NID_STRUCT_USER_ID = "\\{\"type\":\"SET_USER_ID\",\"ts\":(.*?),\"uid\":\"(.*?)\"\\}"
const val NID_STRUCT_REGISTER_TARGET = "\\{\"type\":\"REGISTER_TARGET\",\"tg\":\\{\"attr\":\\[\\{\"n\":\"guid\",\"v\":\"(.*?)\"\\}\\]\\},\"tgs\":\"(.*?)\",\"v\":\"S~C~~\\d{1,}\",\"en\":\"(.*?)\",\"et\":\"(.*?)\",\"eid\":\"(.*?)\",\"ts\":\\d{13,},\"url\":\"(.*?)\"\\}"
const val NID_STRUCT_SLIDER_CHANGE = "\\{\"type\":\"SLIDER_CHANGE\",\"tg\":\\{\"tgs\":\"\",\"etn\":\"(.*?)\"\\},\"v\":\"\\d+\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_FORM_SUBMIT = "\\{\"type\":\"FORM_SUBMIT\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_FORM_SUCCESS = "\\{\"type\":\"FORM_SUBMIT_SUCCESS\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_FORM_ERROR = "\\{\"type\":\"FORM_SUBMIT_FAILURE\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_CUSTOM_EVENT = "\\{\"type\":\"CUSTOM_EVENT\",\"tgs\":\"(.*?)\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_WINDOW_LOAD = "\\{\"type\":\"WINDOW_LOAD\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_WINDOW_FOCUS = "\\{\"type\":\"WINDOW_FOCUS\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_WINDOW_BLUR = "\\{\"type\":\"WINDOW_BLUR\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_WINDOW_UNLOAD = "\\{\"type\":\"WINDOW_UNLOAD\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_TOUCH_START = "\\{\"type\":\"TOUCH_START\",\"tg\":\\{\"tgs\":\"(.*?)\",\"sender\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_TOUCH_END = "\\{\"type\":\"TOUCH_END\",\"ts\":\\d{13,},\"x\":(.*?),\"y\":(.*?)\\}"
const val NID_STRUCT_TOUCH_MOVE = "\\{\"type\":\"TOUCH_MOVE\",\"ts\":\\d{13,},\"x\":(.*?),\"y\":(.*?)\\}"
const val NID_STRUCT_WINDOW_RESIZE = "\\{\"type\":\"WINDOW_RESIZE\",\"ts\":\\d{13,},\"w\":\\d+,\"h\":\\d+\\}"
const val NID_STRUCT_WINDOW_ORIENTATION_CHANGE = "\\{\"type\":\"WINDOW_ORIENTATION_CHANGE\",\"tg\":\\{\"orientation\":\"(?:Landscape|Portrait)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_FOCUS = "\\{\"type\":\"FOCUS\",\"tg\":\\{\"tgs\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_BLUR = "\\{\"type\":\"BLUR\",\"tg\":\\{\"tgs\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_INPUT = "\\{\"type\":\"INPUT\",\"tg\":\\{\"tgs\":\"(.*?)\",\"attr\":\\[\\{\"n\":\"(.*?)\",\"v\":\"S~C~~\\d{1,}\"\\},\\{\"n\":\"(.*?)\",\"v\":\"(.*?)\"\\}\\],\"etn\":\"(.*?)\",\"et\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_TEXT_CHANGE = "\\{\"type\":\"TEXT_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"attr\":\\[\\{\"n\":\"(.*?)\",\"v\":\"S~C~~\\d{1,}\"\\},\\{\"n\":\"(.*?)\",\"v\":\"(.*?)\"\\}\\],\"etn\":\"(.*?)\",\"et\":\"(.*?)\"\\},\"v\":\"S~C~~\\d{1,}\",\"ts\":\\d{13,}\\}"
const val NID_STRUCT_RADIO_CHANGE = "\\{\"type\":\"RADIO_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_CHECKBOX_CHANGE = "\\{\"type\":\"CHECKBOX_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_SELECT_CHANGE = "\\{\"type\":\"SELECT_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\",\"et\":\"(.*?)\"\\},\"ts\":\\d{13,}\\}"
const val NID_STRUCT_USER_INACTIVE = "\\{\"type\":\"USER_INACTIVE\",\"ts\":\\d{13,}\\}"