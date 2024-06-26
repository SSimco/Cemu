#pragma once

#include "input/api/InputAPI.h"

#include <optional>
#include <wx/dialog.h>
#include <wx/panel.h>

#include "helpers/wxCustomData.h"
#include "input/api/Controller.h"

class wxComboBox;
class wxChoice;
class wxTextCtrl;

using wxAPIType = wxCustomData<InputAPI::Type>;

class InputAPIAddWindow : public wxDialog
{
public:
	InputAPIAddWindow(wxWindow* parent, const wxPoint& position, const std::vector<ControllerPtr>& controllers);

	bool is_valid() const { return m_type.has_value() && m_controller != nullptr; }
	InputAPI::Type get_type() const { return m_type.value(); }
	std::shared_ptr<ControllerBase> get_controller() const { return m_controller; }

	bool has_custom_settings() const;
	std::unique_ptr<ControllerProviderSettings> get_settings() const;

private:
	void on_add_button(wxCommandEvent& event);
	void on_close_button(wxCommandEvent& event);
	

	void on_api_selected(wxCommandEvent& event);

	void on_controller_dropdown(wxCommandEvent& event);
	void on_controller_selected(wxCommandEvent& event);
	void on_controllers_refreshed(wxCommandEvent& event);

	wxChoice* m_input_api;
	wxComboBox* m_controller_list;
	wxButton* m_ok_button;

	wxPanel* m_settings_panel;
	wxTextCtrl* m_ip, * m_port;

	std::optional<InputAPI::Type> m_type;
	std::shared_ptr<ControllerBase> m_controller;

	std::vector<ControllerPtr> m_controllers;
	std::atomic_bool m_search_running = false;
};
