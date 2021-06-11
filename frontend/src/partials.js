import { createApp } from 'vue'
import Partials from './Partials.vue'

const app = createApp(Partials);
const el = document.getElementById('partials');
if (el) {
    app.provide('noteid', el.dataset.noteid)
    app.mount('#partials')
}