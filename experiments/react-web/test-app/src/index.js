import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import registerServiceWorker from './registerServiceWorker';

if (window.location.search === "?noauth") {
  setupLogin();
} else {
  ReactDOM.render(<App />, document.getElementById('root'));
  registerServiceWorker();
}


function setupLogin() {
  document.body.innerHTML = "<h1>log in</h1>";
}
