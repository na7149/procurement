
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import DeliverymanagementManager from "./components/DeliverymanagementManager"

import DeliveryrequestManager from "./components/DeliveryrequestManager"
import InspectionResultManager from "./components/InspectionResultManager"

import GoodsdeliveryManager from "./components/GoodsdeliveryManager"


import DeliveryStatusInquiry from "./components/DeliveryStatusInquiry"
import SmsHistoryManager from "./components/SmsHistoryManager"

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/deliverymanagements',
                name: 'DeliverymanagementManager',
                component: DeliverymanagementManager
            },

            {
                path: '/deliveryrequests',
                name: 'DeliveryrequestManager',
                component: DeliveryrequestManager
            },
            {
                path: '/inspectionResults',
                name: 'InspectionResultManager',
                component: InspectionResultManager
            },

            {
                path: '/goodsdeliveries',
                name: 'GoodsdeliveryManager',
                component: GoodsdeliveryManager
            },


            {
                path: '/deliveryStatusInquiries',
                name: 'DeliveryStatusInquiry',
                component: DeliveryStatusInquiry
            },
            {
                path: '/smsHistories',
                name: 'SmsHistoryManager',
                component: SmsHistoryManager
            },



    ]
})
