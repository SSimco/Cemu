#pragma once

<<<<<<< HEAD:src/gui/canvas/VulkanCanvas.h
#include "canvas/IRenderCanvas.h"
=======
#include "wxgui/canvas/IRenderCanvas.h"
>>>>>>> public/main:src/gui/wxgui/canvas/VulkanCanvas.h

#include <wx/frame.h>

#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanAPI.h"
#include <set>

class VulkanCanvas : public IRenderCanvas, public wxWindow
{
#if BOOST_OS_LINUX && HAS_WAYLAND
	std::unique_ptr<class wxWlSubsurface> m_subsurface;
#endif
public:
	VulkanCanvas(wxWindow* parent, const wxSize& size, bool is_main_window);
	~VulkanCanvas();

private:

	void OnPaint(wxPaintEvent& event);
	void OnResize(wxSizeEvent& event);
};
