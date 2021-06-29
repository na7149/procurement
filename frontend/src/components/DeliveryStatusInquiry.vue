<template>

<v-data-table
    :headers="headers"
    :items="deliveryStatusInquiry"
    :items-per-page="5"
    class="elevation-1"
  ></v-data-table>

</template>

<script>
  const axios = require('axios').default;

  export default {
    name: 'DeliveryStatusInquiry',
    props: {
      value: Object,
      editMode: Boolean,
      isNew: Boolean
    },
    data: () => ({
        headers: [
            { text: "id", value: "id" },
            { text: "procNo", value: "procNo" },
            { text: "procTitle", value: "procTitle" },
            { text: "companyNm", value: "companyNm" },
            { text: "inspectionSuccFlag", value: "inspectionSuccFlag" },
        ],
        deliveryStatusInquiry : [],
    }),
    async created() {
      var temp = await axios.get(axios.backend + '/deliverystatusinquiries')

      this.deliveryStatusInquiry = temp.data._embedded.deliverystatusinquiries;

    },
    methods: {
    }
  }
</script>

