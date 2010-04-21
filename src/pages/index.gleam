controller pages.Index

page(title: "Welcome") {
  paragraph {
    text "Hello "
    text c.username
    text "."
  }
  
  paragraph {
    text "It is now "
    text c.time
  }
  
  form {
    field(property: @c.username, label: "Username")
  }
}
