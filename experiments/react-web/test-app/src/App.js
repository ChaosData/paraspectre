import React, { Component } from 'react';
import './App.css';

// using ES6 modules
import { BrowserRouter, Route, Link, Switch, Redirect, withRouter } from 'react-router-dom'

import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import darkBaseTheme from 'material-ui/styles/baseThemes/darkBaseTheme';
import getMuiTheme from 'material-ui/styles/getMuiTheme';

import Drawer from 'material-ui/Drawer';
import {List, ListItem} from 'material-ui/List';
import RaisedButton from 'material-ui/RaisedButton';
import FlatButton from 'material-ui/FlatButton';

import ContentCreate from 'material-ui/svg-icons/content/create';
import ContentAdd from 'material-ui/svg-icons/content/add';
import ExposurePlus from 'material-ui/svg-icons/image/exposure-plus-1';

//const server_url = 'http://127.0.0.1:8088/';

class App extends Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <MuiThemeProvider muiTheme={getMuiTheme(darkBaseTheme)}>
      <BrowserRouter>
        <div>
          <Switch>
            <Route exact path="/" component={Dashboard}/>
            <Route exact path="/:pkg" component={Dashboard}/>
            <Route exact path="/:pkg/:selector" component={Dashboard}/>
            <Route exact path="/:pkg/:selector/:class" component={Dashboard}/>
            <Route exact path="/:pkg/:selector/:class/:method" component={Dashboard}/>
          </Switch>
        </div>
      </BrowserRouter>
      </MuiThemeProvider>
    )
  }
}



class Dashboard extends Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div>
        <div>
          <Drawer docked={true}>
            <List>
              <ListItem primaryText="" style={{backgroundColor:"#a4c639"}}>
                <FlatButton backgroundColor="#a4c639"
                            hoverColor="#8AA62F"
                            icon={<ContentAdd color="rgb(48, 48, 48)" />}
                            style={ {width: "100%"} }
                />
              </ListItem>
              <ListItem className="ListItemCenter">
                <span>com.facebook.katana</span>
              </ListItem>
              <ListItem className="ListItemCenter">
                <span>trust.nccgroup.foo.bar.android.baz.main.ggg</span>
              </ListItem>
            </List>
          </Drawer>
        </div>

        <div className="content-root">
          <h1>pkg: {this.props.match.params.pkg}</h1>
        </div>


      </div>
    )
  }
}

export default App;
